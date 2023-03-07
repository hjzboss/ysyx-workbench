#include <common.h>
#include "syscall.h"

void halt(int code);
void yield();
void insert_strace(char *name, uint64_t *args, uint64_t ret);
void free_strace();
void print_strace();

void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  switch (a[0]) {
    case SYS_yield: insert_strace("SYS_yield", a, c->GPRx); yield(); break;
    //case SYS_exit: insert_strace("SYS_exit", a, c->GPRx); print_strace(); free_strace(); halt(0); break;
    //case SYS_write: break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
