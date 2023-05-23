#include "klib.h"
#include "trap.h"

char fuck[8] = {};

int main() {
  char i;
  //char *ptr = (char *)0x81000000;
  for(i = 0; i < 8; i++) {
    fuck[i] = i;
  }
  for(i = 0; i < 8; i++) {
    check(fuck[i] == i);
  }

  return 0;
}