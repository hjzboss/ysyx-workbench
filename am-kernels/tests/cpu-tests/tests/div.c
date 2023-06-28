#include "trap.h"

#define N 10
int a[N];

int main() {
  int i, j;
  for(i = 0; i < N; i ++)
    a[i] = i;
  for(i = 0; i < N; i ++)
    for(j = 1; j < N + 1; j ++)
      a[i] *= j;
  for(i = 0; i < N; i ++)
    for(j = 1; j < N + 1; j ++)
      a[i] /= j;

  for(i = 0; i < N; i ++)
    check(a[i] == i);

  uint64_t c = 123456;
  uint64_t d = 2;
  uint64_t e = c / d;
  uint64_t f = 61728;
  check(e == f);

  return 0;
}
