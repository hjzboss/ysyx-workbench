#include <cpu/cpu.h>
#include <monitor/sdb.h>

void log_exit();
void delete_cpu();

int main(int argc, char** argv, char** env) {

  // Prevent unused variable warnings
  if (0 && argc && argv && env) {}
  // Pass arguments so Verilated code can see them, e.g. $value$plusargs
  Verilated::commandArgs(argc, argv);

  // Set debug level, 0 is off, 9 is highest presently used
  Verilated::debug(0);

  init_monitor(argc, argv);

  printf("debug\n\n\n\n\n\n\n\n");
  // Simulate until $finish
  sdb_mainloop();

  log_exit();

  delete_cpu();

  // Fin
  exit(0);
}