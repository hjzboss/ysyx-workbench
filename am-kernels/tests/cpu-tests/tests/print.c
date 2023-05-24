#include "trap.h"

int main() {
  //char *shit = "shit";
  //int b = 123;
  //void *a = &b;
  //printf("%s = %p\n", shit, a);
  *(int *)0x85a00000 = 12345;
  short *shit = (short *)0x85a00000;
  printf("fuck=%d\n", *shit);
  return 0;
}