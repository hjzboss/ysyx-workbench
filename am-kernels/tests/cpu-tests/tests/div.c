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

  long long c = 123456;
  long long d = 2;
  long long e = c / d;
  long long f = 61728;
  check(e == f);

  return 0;
}
