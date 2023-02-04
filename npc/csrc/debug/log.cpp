#include <stdio.h>
#include <assert.h>
#include <stdarg.h>
#include <debug.h>

FILE *log_fp = NULL;
static char buff[1000];

void init_log(const char *log_file) {
  printf("log: %s\n", log_file);
  log_fp = stdout;
  if (log_file != NULL) {
    FILE *fp = fopen(log_file, "w");
    Assert(fp, "Can not open '%s'", log_file);
    log_fp = fp;
  }
  Log("Log is written to %s", log_file ? log_file : "stdout");
}

void log_exit() {
  if(log_fp != NULL) {
    fclose(log_fp);
  }
}