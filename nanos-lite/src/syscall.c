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
    case 0x1: insert_strace("SYS_yield", a, c->GPRx); SYS_yield; break;
    case 0x0: insert_strace("SYS_exit", a, c->GPRx); print_strace(); free_strace(); SYS_exit; break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
