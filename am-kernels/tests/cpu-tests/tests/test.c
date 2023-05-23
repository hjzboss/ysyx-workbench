#include "klib.h"
#include "trap.h"

int main() {
  char i;
  char *ptr = (char *)0x81000000;
  for(i = 0; i < 8; i++) {
    *(char *)(ptr + i) = i;
  }
  for(i = 0; i < 8; i++) {
    char a = *(char *)(ptr + i);
    if(a != i) {
      printf("shit\n");
    }
    check(a == i);
  }

  return 0;
}