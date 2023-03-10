#include <unistd.h>
#include <sys/time.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

int main() {
  struct timeval *tv = (struct timeval *)malloc(sizeof(struct timeval));
  uint64_t old_usec = 0;
  while (gettimeofday(tv, NULL) == 0) {
    uint64_t usec = tv->tv_usec;
    printf("%lu\n", usec);
    if (usec - old_usec == 500000) {
      printf("test\n");
      old_usec = usec;
    }
  }
  return 0;
}
