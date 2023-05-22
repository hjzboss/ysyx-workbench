#include "trap.h"

int main() {
  //char *shit = "shit";
  //int b = 123;
  //void *a = &b;
  //printf("%s = %p\n", shit, a);
  *(int *)0x81a00000 = 12345;
  int *shit = (int *)0x81a00000;
  printf("fuck=%d\n", *shit);
  return 0;
}