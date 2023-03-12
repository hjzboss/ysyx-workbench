#include "fixedptc.h"
#include <stdio.h>

int main() {
  fixedpt A = fixedpt_rconst(-1.2);
  printf("%x\n", A);
  printf("%x\n", fixedpt_floor(A));
  printf("%x\n", fixedpt_ceil(A));
  return 0;
}