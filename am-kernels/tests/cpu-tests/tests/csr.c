#include "trap.h"
#include <stdio.h>

void fuck() {
  asm volatile(
    "csrrci t0, mepc, 0\n"
    "addi t0, t0, 4\n"
    "csrrw t0, mepc, t0\n"
    :
    :
    : "t0"
  );
  printf("syscall\n");
  asm volatile("mret");
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
