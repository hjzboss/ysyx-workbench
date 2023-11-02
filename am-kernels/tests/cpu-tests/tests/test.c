#include "klib.h"
#include "trap.h"

int fuck[65535] = {};

int main() {
  int i;
  //char *ptr = (char *)0x81000000;
  for(i = 0; i < 65535; i++) {
    fuck[i] = rand() * i;
  }
  /*
  for(i = 0; i < 65535; i++) {
    check(fuck[i] == 1);
  }

  i = 0;
  while(i < 65535) {
    i++;
  }*/

  return 0;
}