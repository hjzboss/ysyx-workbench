#include <common.h>
#include "syscall.h"

void syscall_exit(Context *c, uintptr_t *a) {
  insert_strace("SYS_exit", a, c->GPRx);
  print_strace();
  free_strace();
  halt(0);
}

void syscall_yield(Context *c, uintptr_t *a) {
  insert_strace("SYS_yield", a, c->GPRx);
  yield();
}

void syscall_write(Context *c, uintptr_t *a) {
  insert_strace("SYS_write", a, c->GPRx);
  print_strace();
  free_strace();
  halt(0);
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
    case SYS_write: syscall_write(c, a); break; // todo
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
