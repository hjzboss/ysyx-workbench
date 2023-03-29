#include "trap.h"

int main() {
  char *shit = "shit";
  int b = 123;
  void *a = &b;
  printf("%s = %p\n", shit, a);
  return 0;
}