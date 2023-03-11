#include <unistd.h>
#include <sys/time.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <NDL.h>

int main() {
  NDL_Init(0);
  uint32_t old_ms = 0;
  while (1) {
    uint32_t ms = NDL_GetTicks();
    if (ms - old_ms >= 500) {
      printf("test\n");
      old_ms = ms;
    }
  }
  NDL_Quit();
  return 0;
}
