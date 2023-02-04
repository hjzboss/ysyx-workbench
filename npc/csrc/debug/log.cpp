#include <stdio.h>
#include <assert.h>
#include <stdarg.h>

FILE *log_fp = NULL;
static char buff[1000];

void log_init(char *file) {
  log_fp = fopen(file, "w+");
  if(log_fp == NULL) {
    printf("can't open or create log file: %s\n", file);
    assert(0);
  }
}

void log_exit() {
  if(log_fp != NULL) {
    fclose(log_fp);
  }
}

void log_write(bool print_screen, const char *fmt, ...) {
  assert(fmt);
  buff[0] = '\0';

  va_list ap;
  va_start(ap, fmt);
  int n = vsprintf(buff, fmt, ap);
  va_end(ap);

  if(log_fp != NULL) {
    // write to log file
    fprintf(log_fp, "%s", buff);
  }
  // print to screen
  if(print_screen) {
    printf("%s", buff);
  }
}