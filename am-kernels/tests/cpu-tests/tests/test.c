#include "klib.h"
#include "trap.h"

int fuck[65535] = {};

int main() {
  int i;
  //char *ptr = (char *)0x81000000;
  for(i = 0; i < 200000; i++) {
    fuck[i] = 1;
  }
  for(i = 0; i < 200000; i++) {
    check(fuck[i] == 1);
  }

  return 0;
}