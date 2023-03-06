#include <elf.h>
#include <common.h>
#include <utils.h>

#ifdef CONFIG_FTRACE
#define MAX_FUNC_NAME_WIDTH 50
#define MAX_FUNC_NUM 5000
#define ECALL 0x73
#define MRET 0x30200073
enum func_type {
  CALL, RET, OTHER
};

typedef struct {
  int id;
  size_t size;
  vaddr_t start_addr;
  char name[MAX_FUNC_NAME_WIDTH]; // function name
} func;

static func func_list[MAX_FUNC_NUM]; // function list

static int func_num = 0; // function count

typedef struct node {
  /* data */
  int type;
  paddr_t addr;
  int func_no; // 函数类型
  paddr_t obj_addr; // 目标地址
  struct node *next;
} fringbuf;

static fringbuf *ftrace_head = NULL;
static fringbuf *ftrace_tail = NULL;

static void insert_ftrace(int type, paddr_t addr, int func_no, paddr_t next_pc) {
  fringbuf *node = (fringbuf*)malloc(sizeof(fringbuf));
  node->type = type;
  node->addr = addr;
  node->func_no = func_no;
  node->obj_addr = next_pc;
  node->next = NULL;

  if (ftrace_head == NULL) {
    ftrace_head = node;
    ftrace_tail = node;
  }
  else {
    ftrace_tail->next = node;
    ftrace_tail = node;
  }
}

void free_ftrace() {
  fringbuf *tmp;
  while(ftrace_head != NULL) {
    tmp = ftrace_head->next;
    free(ftrace_head);
    ftrace_head = tmp;
  }
}

void print_ftrace(bool log) {
  if(log) log_write("---ftrace message start---\n");
  else printf("---ftrace message start---\n");
  fringbuf *ptr = ftrace_head;
  while(ptr != NULL) {
    // log message
    if (ptr->type == CALL) {
      if(log) log_write("0x%016x: call [%s@0x%016x]\n", ptr->addr, func_list[ptr->func_no].name, ptr->obj_addr);
      else printf("0x%016x: call [%s@0x%016x]\n", ptr->addr, func_list[ptr->func_no].name, ptr->obj_addr);
    }
    else {
      if(log) log_write("0x%016x: ret [%s@0x%016x]\n", ptr->addr, func_list[ptr->func_no].name, ptr->obj_addr);
      else printf("0x%016x: ret [%s@0x%016x]\n", ptr->addr, func_list[ptr->func_no].name, ptr->obj_addr);
    }
    ptr = ptr->next;
  }
  if(log) log_write("---ftrace message end---\n");
  else log_write("---ftrace message end---\n");
}


void init_elf(const char *file) {
  printf("read elf file\n");
  FILE *fp;
  fp = fopen(file, "r");
  if (NULL == fp) {
    printf("fail to open the file\n");
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

  assert(*(uint32_t *)elf_head.e_ident == 0x464C457F);
  printf("shit\n");
  // elf magic number
  if (elf_head.e_ident[0] != 0x7F ||
    elf_head.e_ident[1] != 'E' ||
    elf_head.e_ident[2] != 'L' ||
    elf_head.e_ident[3] != 'F') {
    printf("Not a ELF file\n");
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
        printf("ftrace: function name is too long, the excess will be stage\n");
        name[j-10] = '\0';
      }
      // limit num of func list
      if(func_num == MAX_FUNC_NUM) {
        printf("ftrace: function is too much, more than %d!\n", MAX_FUNC_NUM);
        assert(0);
      }
      func_list[func_num].id = func_num;
      func_list[func_num].start_addr = sym->st_value;
      if (strcmp(name, "__am_asm_trap") == 0)
        func_list[func_num].size = 312; // todo
      else
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
    printf("no function in elf file!\n");
    assert(0);
  }
  printf("read elf file finfished\n");
}

// pc in which function
static int find_func(paddr_t pc) {
  for (int i = 0; i < func_num; i++) {
    if(pc == func_list[i].start_addr || ((pc >= func_list[i].start_addr) && (pc < (func_list[i].start_addr + func_list[i].size)))) {
      return i;
    }
  }
  printf("0x%016x no funciton match!\n", pc);
  print_ftrace(true);
  assert(0);
  return 0;
}

// check the function type
static int check_func_type(uint32_t inst) {
  if (inst == ECALL) return CALL;
  if (inst == MRET) return RET;
  uint8_t op = inst & 0x07f;
  uint8_t rd = (inst >> 7) & 0x1f;
  uint8_t rs1 = (inst >> 15) & 0x1f;
  bool rs1_link = rs1 == 0x1 || rs1 == 0x5;
  bool rd_link = rd == 0x1 || rd == 0x5;
  // jalr
  if (op == 0x67) {
    if (!rd_link && rs1_link) return RET;
    else if (rs1 == rd && rd_link && rs1_link) return CALL;
    else if (!rs1_link && rd_link) return CALL;
  }
  else if (op == 0x6f && rd_link) return CALL;

  return OTHER;
}


void ftrace(paddr_t addr, uint32_t inst, paddr_t next_pc) {
  int type = check_func_type(inst);
  if (type == OTHER) return;
  else if (type == CALL)
    insert_ftrace(CALL, addr, find_func(next_pc), next_pc);
  else
    insert_ftrace(RET, addr, find_func(next_pc), next_pc);
}
#endif