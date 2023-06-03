#include <stdio.h>
#include <assert.h>
#include <stdarg.h>
#include <debug.h>

FILE *npc_log_fp = NULL;

void npc_init_log(const char *log_file) {
  printf("log: %s\n", log_file);
  npc_log_fp = stdout;
  if (log_file != NULL) {
    FILE *fp = fopen(log_file, "w");
    Assert(fp, "Can not open '%s'", log_file);
    npc_log_fp = fp;
  }
  Log("Log is written to %s", log_file ? log_file : "stdout");
}

void log_exit() {
  if(npc_log_fp != NULL) {
    fclose(npc_log_fp);
  }
}