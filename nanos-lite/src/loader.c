#include <proc.h>
#include <elf.h>
#include <klib.h>

int fs_open(const char *pathname, int flags, int mode);
size_t fs_read(int fd, void *buf, size_t len);
size_t fs_write(int fd, const void *buf, size_t len);
size_t fs_lseek(int fd, size_t offset, int whence);
int fs_close(int fd);

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);
size_t get_ramdisk_size();

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

static uintptr_t loader(PCB *pcb, const char *filename) {
  int fd = fs_open(filename, 0, 0);
  Elf_Ehdr elf_head;
  fs_read(fd, &elf_head, sizeof(Elf_Ehdr));
  //ramdisk_read(&elf_head, 0, sizeof(Elf_Ehdr));
  // elf magic number assert
  assert(*(uint32_t *)elf_head.e_ident == 0x464C457F);

  // read phdr head
  Elf_Phdr *p_head = (Elf_Phdr *)malloc(sizeof(Elf_Phdr) * elf_head.e_phnum);
  fs_read(fd, p_head, sizeof(Elf_Phdr) * elf_head.e_phnum);
  //ramdisk_read(p_head, elf_head.e_phoff, sizeof(Elf_Phdr) * elf_head.e_phnum);

  // load and set
  for (int i = 0; i < elf_head.e_phnum; i++, p_head++) {
    printf("p_head->p_filesz=%p\n", p_head->p_filesz);
    if (p_head->p_type == PT_LOAD) {
      //printf("p_head->p_filesz=%u\n", p_head->p_filesz);
      fs_read(fd, (uint8_t *)p_head->p_vaddr, p_head->p_filesz);
      //ramdisk_read((uint8_t *)p_head->p_vaddr, p_head->p_offset, p_head->p_filesz);
      memset((uint8_t *)p_head->p_vaddr + p_head->p_filesz, 0, p_head->p_memsz - p_head->p_filesz);      
    }
    else continue;
    //printf("p_head->p_filesz=%u\n", p_head->p_filesz);
    //fs_read(fd, (uint8_t *)p_head->p_vaddr, p_head->p_filesz);
    //ramdisk_read((uint8_t *)p_head->p_vaddr, p_head->p_offset, p_head->p_filesz);
    //memset((uint8_t *)p_head->p_vaddr + p_head->p_filesz, 0, p_head->p_memsz - p_head->p_filesz);
  }

  fs_close(fd);

  return elf_head.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}

