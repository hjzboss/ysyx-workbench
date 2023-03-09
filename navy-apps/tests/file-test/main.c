#include <stdio.h>
#include <assert.h>

int main() {
  printf("fuck0\n");
  FILE *fp = fopen("/share/files/num", "r+");
  assert(fp);
  printf("fuck\n");
  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);
  assert(size == 5000);
  printf("fuck1\n");
  fseek(fp, 500 * 5, SEEK_SET);
  int i, n;
  printf("shit\n");
  for (i = 500; i < 1000; i ++) {
    fscanf(fp, "%d", &n);
    assert(n == i + 1);
  }
  printf("shit1\n");
  fseek(fp, 0, SEEK_SET);
  for (i = 0; i < 500; i ++) {
    fprintf(fp, "%4d\n", i + 1 + 1000);
  }
  printf("shit2\n");
  for (i = 500; i < 1000; i ++) {
    fscanf(fp, "%d", &n);
    assert(n == i + 1);
  }
  printf("shit3\n");
  fseek(fp, 0, SEEK_SET);
  for (i = 0; i < 500; i ++) {
    fscanf(fp, "%d", &n);
    assert(n == i + 1 + 1000);
  }
  printf("shit4\n");
  fclose(fp);
  printf("shit5\n");
  printf("PASS!!!\n");

  return 0;
}
