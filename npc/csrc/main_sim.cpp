#include <verilated.h>

// Include model header, generated from Verilating "jzcore.v"
#include "VJzCore.h"

// If "verilator --trace" is used, include the tracing class
#include <verilated_vcd_c.h>

// dpi-c
#include "svdpi.h"

#include <stdio.h>

#include <assert.h>

// Current simulation time (64-bit unsigned)
vluint64_t main_time = 0;
// Called by $time in Verilog
double sc_time_stamp () {
  return main_time; // Note does conversion to real, to match SystemC
}

static uint8_t i_cache[65535] = {};

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

static int npc_state;

static void init_cache(char *dir) {
  FILE *fp = fopen(dir, "rb");

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(i_cache, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
}

static uint32_t pmem_read(uint64_t pc) {
  return *(uint32_t *)(i_cache + pc);
}

/*
void exec_single(VJzCore* dut, VerilatedVcdC* tfp) {
  bool flag = true;
  // Simulate until $finish
  while (!Verilated::gotFinish() && (main_time <= MAX_SIM_TIME) && flag) {
    if ((main_time % 10) == 0) { // 1 cycle is 10 ns
      dut->clock = 1;
    }
    if ((main_time % 10) == 5) {
      dut->clock = 0;
      flag = 0;
    }
    //dut->io_inst = pmem_read(dut->io_pc);
    // Evaluate model
    dut->eval();
    tfp->dump(main_time); // dump wave
    main_time++;  // Time passes...
  }
}
*/
#define MAX_SIM_TIME 1000 //max simulation time

// for ebreak instruction
extern "C" void c_break() {
  npc_state = NPC_END;
}

int main(int argc, char** argv, char** env) {
  // This is a more complicated example, please also see the simpler examples/hello_world_c.

  // Prevent unused variable warnings
  if (0 && argc && argv && env) {}
  // Pass arguments so Verilated code can see them, e.g. $value$plusargs
  Verilated::commandArgs(argc, argv);

  // Set debug level, 0 is off, 9 is highest presently used
  Verilated::debug(0);

  // Randomization reset policy
  //Verilated::randReset(2);

  // Construct the Verilated model, from Vjzcore.h generated from Verilating "jzcore.v"
  VJzCore* jzcore = new VJzCore; // Or use a const unique_ptr, or the VL_UNIQUE_PTR wrapper

  VerilatedVcdC* tfp = NULL;
  Verilated::traceEverOn(true);  // Verilator must compute traced signals
  tfp = new VerilatedVcdC;
  jzcore->trace(tfp, 99);  // Trace 99 levels of hierarchy
  tfp->open("/home/hjz/ysyx-workbench/npc/build/sim/obj_dir/wave.vcd");  // Open the dump file

  // initial i_cache
  init_cache(argv[1]);

  // Set some inputs
  jzcore->reset = 1;

  // state is running
  npc_state = NPC_RUNNING;
  
  // Simulate until $finish
  while (!Verilated::gotFinish() && (main_time <= MAX_SIM_TIME)) {

    if(main_time > 3){
      jzcore->reset = 0;
    }
    if ((main_time & 0x01) == 0) { // 1 cycle is 10 ns
      if (npc_state == NPC_END) break;
      jzcore->clock = 1;
    }
    if ((main_time & 0x01) == 1) {
      jzcore->clock = 0;
    }
    jzcore->io_inst = pmem_read(jzcore->io_pc);
    // Evaluate model
    jzcore->eval();
    tfp->dump(main_time); // dump wave
    main_time++;  // Time passes...
  }

  // Final model cleanup
  jzcore->final();

  // Close trace if opened
  if (tfp) { tfp->close(); }

  // Destroy model
  delete jzcore; jzcore = NULL;

  if (npc_state == NPC_END) {
    printf("--------------------------HIT GOOD TRAP------------------------\n");
  }
  else {
    printf("---------------------------HIT BAD TRAP------------------------\n");
  }
  // Fin
  exit(0);
}