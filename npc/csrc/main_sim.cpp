#include "include/cpu/cpu.h"

int main(int argc, char** argv, char** env) {

  // Prevent unused variable warnings
  if (0 && argc && argv && env) {}
  // Pass arguments so Verilated code can see them, e.g. $value$plusargs
  Verilated::commandArgs(argc, argv);

  // Set debug level, 0 is off, 9 is highest presently used
  Verilated::debug(0);

  init_cpu(argv[1]);

  // Simulate until $finish
  main_loop();

  if (npc_state == NPC_END) {
    printf("--------------------------HIT GOOD TRAP------------------------\n");
  }
  else {
    printf("---------------------------HIT BAD TRAP------------------------\n");
  }

  delete_cpu();

  // Fin
  exit(0);
}