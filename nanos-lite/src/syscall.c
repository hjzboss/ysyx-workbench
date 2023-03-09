#include <common.h>
#include "syscall.h"

#ifdef CONFIG_STRACE
void insert_strace(char *name, uint64_t *args, uint64_t ret, int fd);
void free_strace();
void print_strace();
#endif

void halt(int code);
void yield();
void putch(char ch);
int fs_open(const char *pathname, int flags, int mode);
size_t fs_read(int fd, void *buf, size_t len);
size_t fs_write(int fd, const void *buf, size_t len);
size_t fs_lseek(int fd, size_t offset, int whence);
int fs_close(int fd);


void syscall_exit(Context *c, uintptr_t *a) {
#ifdef CONFIG_STRACE
  insert_strace("SYS_exit", a, c->GPRx, -1);
  print_strace();
  free_strace();
#endif
  halt(0);
}

void syscall_yield(Context *c, uintptr_t *a) {
#ifdef CONFIG_STRACE
  insert_strace("SYS_yield", a, c->GPRx, -1);
#endif
  yield();
}

void syscall_brk(Context *c, uintptr_t *a) {
  // todo, 此时的返回值总是返回0，代表总是分配成功
  c->GPRx = 0;
#ifdef CONFIG_STRACE
  insert_strace("SYS_brk", a, c->GPRx, -1);
#endif
}

void syscall_write(Context *c, uintptr_t *a) {
  int fd = a[1];
  uint8_t *buf = (uint8_t *)a[2];
  int len = a[3];
  c->GPRx = -1;
  if (fd == 1 || fd == 2) {
    for (int i = 0; i < len; i++, buf++) {
      putch(*buf);
    }
    c->GPRx = len;
#ifdef CONFIG_STRACE
  insert_strace("SYS_write", a, c->GPRx, -1);
#endif 
  }
  else {
    // file
    c->GPRx = fs_write(fd, buf, len);
#ifdef CONFIG_STRACE
    insert_strace("SYS_write", a, c->GPRx, fd);
#endif
  }
}

void syscall_read(Context *c, uintptr_t *a) {
  int fd = a[1];
  uint8_t *buf = (uint8_t *)a[2];
  int len = a[3];
  c->GPRx = -1;
  if (fd == 1 || fd == 2) {
    panic("doesn't implement");
  }
  else {
    c->GPRx = fs_read(fd, buf, len);
#ifdef CONFIG_STRACE
  insert_strace("SYS_read", a, c->GPRx, fd);
#endif
  }
}

void syscall_lseek(Context *c, uintptr_t *a) {
  int fd = a[1];
  size_t offset = a[2];
  int whence = a[3];
  c->GPRx = fs_lseek(fd, offset, whence);
#ifdef CONFIG_STRACE
  insert_strace("SYS_lseek", a, c->GPRx, fd);
#endif
}

void syscall_open(Context *c, uintptr_t *a) {
  char *path = (char *)a[1];
  int flags = a[2];
  unsigned int mode = a[3];
  c->GPRx = fs_open(path, flags, mode);
#ifdef CONFIG_STRACE
  insert_strace("SYS_open", a, c->GPRx, c->GPRx);
#endif
}

void syscall_close(Context *c, uintptr_t *a) {
  int fd = a[1];
  c->GPRx = fs_close(fd);
#ifdef CONFIG_STRACE
  insert_strace("SYS_close", a, c->GPRx, fd);
#endif
}


void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  switch (a[0]) {
    case SYS_yield: syscall_yield(c, a); break;
    case SYS_exit: syscall_exit(c, a); break;
    case SYS_write: syscall_write(c, a); break;
    case SYS_brk: syscall_brk(c, a); break;
    case SYS_read: syscall_read(c, a); break;
    case SYS_lseek: syscall_lseek(c, a); break;
    case SYS_open: syscall_open(c, a); break;
    case SYS_close: syscall_close(c, a); break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
