#include <elf.h>
#include <common.h>

#ifdef CONFIG_FTRACE
#define MAX_FUNC_NAME_WIDTH 50
#define MAX_FUNC_NUM 50

typedef struct {
  int id;
  size_t size;
  vaddr_t start_addr;
  char name[MAX_FUNC_NAME_WIDTH]; // function name
} func;

static func func_list[MAX_FUNC_NUM]; // function list

static int func_num = 0; // function count

void init_elf(const char *file) {
  log_write("read elf file\n");
  FILE *fp;
  fp = fopen(file, "r");
  if (NULL == fp) {
    log_write("fail to open the file\n");
    assert(0);
  }
  // analysis head
  Elf64_Ehdr elf_head;
  int a;
  int i, j;
  char name[MAX_FUNC_NAME_WIDTH] = {};

  // read elf head
  a = fread(&elf_head, sizeof(Elf64_Ehdr), 1, fp);
  assert(a != 0);

  // elf magic number
  if (elf_head.e_ident[0] != 0x7F ||
    elf_head.e_ident[1] != 'E' ||
    elf_head.e_ident[2] != 'L' ||
    elf_head.e_ident[3] != 'F') {
    log_write("Not a ELF file\n");
    assert(0);
  }

  // section header table指针，大小为表中的条目数
  Elf64_Shdr *start1 = (Elf64_Shdr*)malloc(sizeof(Elf64_Shdr) * elf_head.e_shnum);
  assert(NULL != start1);
  Elf64_Shdr *shdr = start1;

  Elf64_Shdr *sym_shdr = NULL; // 符号表
  Elf64_Shdr *str_shdr = NULL; // 字符串表

  // set offset
  a = fseek(fp, elf_head.e_shoff, SEEK_SET);
  assert(a == 0);
  // read section to shdr, size : shdr * num
  a = fread(shdr, sizeof(Elf64_Shdr) * elf_head.e_shnum, 1, fp);
  assert(a != 0);

  // find symbol table
  for (i = 0; i < elf_head.e_shnum; i++) {
    if (shdr->sh_type == 2) {
      sym_shdr = shdr;
    }
    else if (shdr->sh_type == 3) {
      // get first resdult when there are 2 section (type is 3)
      // .strtab is right and .shstrtab is wrong
      if (str_shdr == NULL) {
        str_shdr = shdr;
      }
    }
    shdr++;
  }
  assert(sym_shdr && str_shdr);

  Elf64_Sym *start2 = (Elf64_Sym*)malloc(sym_shdr->sh_size);
  assert(start2 != NULL);
  Elf64_Sym *sym = start2;

  // function numer
  int sym_num = sym_shdr->sh_size / 24;
  // point the file pointer to the beginning
  rewind(fp);
  fseek(fp, sym_shdr->sh_offset, SEEK_SET);
  a = fread(sym, sym_shdr->sh_size, 1,  fp);
  assert(a != 0);

  // 查找符号表将函数添加到func_list中
  for(i = 0; i < sym_num; i++) {
    // symbol type is function
    if ((sym->st_info & 0xf) == 2) {
      rewind(fp);
      a = fseek(fp, str_shdr->sh_offset + sym->st_name, SEEK_SET);
      assert(a == 0);
      a = fread(name, MAX_FUNC_NAME_WIDTH, 1, fp);
      assert(a != 0);
      for (j = 0; j < MAX_FUNC_NAME_WIDTH; j++) {
        if (name[j] == '\0') break;
      }
      if (j == MAX_FUNC_NAME_WIDTH) {
        log_write("ftrace: function name is too long, the excess will be stage\n");
        name[j-10] = '\0';
      }
      // limit num of func list
      if(func_num == MAX_FUNC_NUM) {
        log_write("ftrace: function is too much, more than %d!\n", MAX_FUNC_NUM);
        assert(0);
      }
      func_list[func_num].id = func_num;
      func_list[func_num].start_addr = sym->st_value;
      func_list[func_num].size = sym->st_size;
      strcpy(func_list[func_num].name, name);
      func_num++;
    }
    sym++;
  }
  free(start1);
  free(start2);
  start1 = NULL;
  start2 = NULL;
  fclose(fp);
  if(func_num == 0) {
    log_write("no function in elf file!\n");
    assert(0);
  }
  //func_state = -1;
  log_write("read elf file finfished\n");
}

/*
// pc in which function
static int func_pc(vaddr_t addr) {
  for (int i = 0; i < func_num; i++) {
    if(addr == func_list[i].start_addr || ((addr >= func_list[i].start_addr) && (addr < (func_list[i].start_addr + func_list[i].size)))) {
      return i;
    }
  }
  printf("0x%016lx no funciton match!\n", addr);
  assert(0);
  return 0;
}
*/

#endif