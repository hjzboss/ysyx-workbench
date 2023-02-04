#include <stdio.h>
#include <assert.h>
#include <stdarg.h>
#include <debug.h>

FILE *log_fp = NULL;
static char buff[1000];

void init_log(const char *log_file) {
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