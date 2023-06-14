#include "klib.h"
#include "trap.h"


int main() {
  uint64_t a = 234;
  uint64_t b = 123131;
  uint32_t c = (uint32_t)a * (uint32_t)b;

  printf("%d\n", c);
}