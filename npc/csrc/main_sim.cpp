#include <verilated.h>

// Include model header, generated from Verilating "jzcore.v"
#include "VJzCore.h"

// If "verilator --trace" is used, include the tracing class
#include <verilated_vcd_c.h>

// dpi-c
#include "svdpi.h"

#include <stdio.h>

#include <assert.h>

#define MAX_SIM_TIME 1000 //max simulation time

// Current simulation time (64-bit unsigned)
vluint64_t main_time = 0;

static VJzCore* top;
static VerilatedContext* contextp = NULL;
static VerilatedVcdC* tfp = NULL;

// Called by $time in Verilog
double sc_time_stamp () {
  return main_time; // Note does conversion to real, to match SystemC
}

static uint8_t i_cache[65535] = {};

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

int npc_state;

/*
void eval_wave() {

}
*/

// for ebreak instruction
extern "C" void c_break() {
  npc_state = NPC_END;
}

static uint32_t pmem_read(uint64_t pc) {
  return *(uint32_t *)(i_cache + pc);
}

static void init_cache(char *dir) {
  FILE *fp = fopen(dir, "rb");

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(i_cache, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
}


void one_cycle() {
  top->clock = 1;
  top->io_inst = pmem_read(top->io_pc);
  top->eval();
  tfp->dump(main_time);
  main_time++;

  if (main_time == 3) top->reset = 0;
  top->clock = 0;
  top->io_inst = pmem_read(top->io_pc);
  top->eval();
  tfp->dump(main_time);
  main_time++;    
}

static void init_wave() {
  Verilated::traceEverOn(true);
  tfp = new VerilatedVcdC;
  top->trace(tfp, 99);  // Trace 99 levels of hierarchy
  tfp->open("/home/hjz/ysyx-workbench/npc/build/sim/obj_dir/wave.vcd");  // Open the dump file
}

void init_cpu(char *dir) {
  // Construct the Verilated model, from Vjzcore.h generated from Verilating "jzcore.v"
  top = new VJzCore; // Or use a const unique_ptr, or the VL_UNIQUE_PTR wrapper

  init_wave();

  // initial i_cache
  init_cache(dir);
}

void delete_cpu() {
  // Final model cleanup
  top->final();

  // Close trace if opened
  if (tfp) { tfp->close(); }

  // Destroy model
  delete top; 
  top = NULL;
}

int main(int argc, char** argv, char** env) {

  // Prevent unused variable warnings
  if (0 && argc && argv && env) {}
  // Pass arguments so Verilated code can see them, e.g. $value$plusargs
  Verilated::commandArgs(argc, argv);

  // Set debug level, 0 is off, 9 is highest presently used
  Verilated::debug(0);

  // Randomization reset policy
  //Verilated::randReset(2);

  init_cpu(argv[1]);
  /*
  // Construct the Verilated model, from Vjzcore.h generated from Verilating "jzcore.v"
  top = new VJzCore; // Or use a const unique_ptr, or the VL_UNIQUE_PTR wrapper

  VerilatedVcdC* tfp = NULL;
  Verilated::traceEverOn(true);  // Verilator must compute traced signals
  tfp = new VerilatedVcdC;
  jzcore->trace(tfp, 99);  // Trace 99 levels of hierarchy
  tfp->open("/home/hjz/ysyx-workbench/npc/build/sim/obj_dir/wave.vcd");  // Open the dump file

  // initial i_cache
  init_cache(argv[1]);
*/
  // Set some inputs
  top->reset = 1;

  // state is running
  npc_state = NPC_RUNNING;

  // Simulate until $finish
  while (!Verilated::gotFinish() && (main_time <= MAX_SIM_TIME) && (npc_state == NPC_RUNNING)) {
    if(main_time > 2){
      top->reset = 0;
    }

    one_cycle();


    /*
    if ((main_time & 0x01) == 0) { // 1 cycle is 10 ns
      if (npc_state == NPC_END) break;
      jzcore->clock = 1;
    }
    if ((main_time & 0x01) == 1) {
      jzcore->clock = 0;
    }
    // jzcore->io_inst = pmem_read(jzcore->io_pc);
    // Evaluate model
    jzcore->eval();
    tfp->dump(main_time); // dump wave
    main_time++;  // Time passes...
    */
  }

  if (npc_state == NPC_END) {
    printf("--------------------------HIT GOOD TRAP------------------------\n");
  }
  else {
    printf("---------------------------HIT BAD TRAP------------------------\n");
  }

  delete_cpu();
  /*
  // Final model cleanup
  jzcore->final();

  // Close trace if opened
  if (tfp) { tfp->close(); }

  // Destroy model
  delete jzcore; jzcore = NULL;

  // Fin
  exit(0);
  */
}