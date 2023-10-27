#include "trap.h"
#include <stdio.h>

void fuck() {
  printf("syscall\n");
  //asm volatile("mret");
}

int main() {
  uint8_t a = 0;
  //void (*tmp)() = fuck;
  asm volatile(
    "csrrw t0, mtvec, %[fuck]\n"
    "csrrw t1, mepc, t0\n"
    "ecall\n"
    "csrrc t0, mepc, %[a]\n"
    "ebreak\n"
    :
    : [fuck] "r" (fuck), [a] "r" (a)
    : "t0", "t1"
  );
  return 0;
}
