#include "klib.h"
#include "trap.h"

int fuck[65535] = {};

int main() {
  int i;
  //char *ptr = (char *)0x81000000;
  for(i = 0; i < 260; i++) {
    fuck[i] = i;
  }
  for(i = 0; i < 260; i++) {
    check(fuck[i] == i);
  }

  return 0;
}