#include <common.h>
#include "syscall.h"

void halt(int code);
void yield();

void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  switch (a[0]) {
    case 0x1: SYS_yield; break;
    case 0x0: 
      SYS_exit; 
      break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
