#include "klib.h"
#include "trap.h"

int fuck[12345] = {};

int main() {
  int i;
  //char *ptr = (char *)0x81000000;
  for(i = 0; i < 12345; i++) {
    fuck[i] = i;
  }
  for(i = 0; i < 12345; i++) {
    check(fuck[i] == i);
  }

  return 0;
}