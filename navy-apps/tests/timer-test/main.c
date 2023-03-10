#include <unistd.h>
#include <sys/time.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

uint32_t NDL_GetTicks() {
  struct timeval *tv = (struct timeval *)malloc(sizeof(struct timeval));
  gettimeofday(tv, NULL);
  return tv->tv_usec / 1000;
}

int main() {
  uint32_t old_ms = 0;
  while (1) {
    uint32_t ms = NDL_GetTicks();
    if (ms - old_ms >= 500) {
      printf("test\n");
      old_ms = ms;
    }
  }
  return 0;
}
