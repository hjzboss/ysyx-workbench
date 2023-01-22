#include <verilated.h>

// Include model header, generated from Verilating "jzcore.v"
#include "VJzCore.h"

// If "verilator --trace" is used, include the tracing class
#include <verilated_vcd_c.h>

// dpi-c
#include "svdpi.h"

// Current simulation time (64-bit unsigned)
vluint64_t main_time = 0;
// Called by $time in Verilog
double sc_time_stamp () {
  return main_time; // Note does conversion to real, to match SystemC
}

static uint32_t instr_cache[65535] = {};

static bool is_running;

static void init_cache() {
  instr_cache[0] = 0x00138393;
  instr_cache[4] = 0x00238393;
  instr_cache[8] = 0x00338393;
  instr_cache[12] = 0x00100073;
}

static uint32_t pmem_read(uint64_t pc) {
  return instr_cache[pc];
}

#define MAX_SIM_TIME 100 //max simulation time

// for ebreak instruction
extern "C" void c_stop() {
  printf("c: call stop\n");
  is_running = false;
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
  tfp->open("./build/sim/obj_dir/wave.vcd");  // Open the dump file

  // initial memory
  init_cache();

  // Set some inputs
  jzcore->reset = 1;

  // state is running
  is_running = true;

  // Simulate until $finish
  while (!Verilated::gotFinish() && (main_time <= MAX_SIM_TIME) && is_running) {

    // reset signal remains for 1000 ns(100 cycles)
    if(main_time > 15){
      jzcore->reset = 0;
    }
    if ((main_time % 10) == 0) { // 1 cycle is 10 ns
      jzcore->clock = 1;
    }
    if ((main_time % 10) == 5) {
      jzcore->clock = 0;
    }
    jzcore->io_inst = pmem_read(jzcore->io_pc);
    // Evaluate model
    jzcore->eval();
    //jzcore->io_inst = pmem_read(jzcore->io_pc);
    tfp->dump(main_time);//dump wave
    main_time++;  // Time passes...
  }

  // Final model cleanup
  jzcore->final();

  // Close trace if opened
  if (tfp) { tfp->close(); }

  // Destroy model
  delete jzcore; jzcore = NULL;

  // Fin
  exit(0);
}