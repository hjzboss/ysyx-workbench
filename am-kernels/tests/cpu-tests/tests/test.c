#include "klib.h"
#include "trap.h"


int main() {
  int i;
  //char *ptr = (char *)0x81000000;
  int fuck[65535] = {};
  printf("fuck=%p\n", fuck);
  for(i = 0; i < 65535; i++) {
    fuck[i] = i;
  }
  for(i = 0; i < 65535; i++) {
    check(fuck[i] == i);
  }

  i = 0;
  while(i < 65535) {
    i++;
  }

  return 0;
}