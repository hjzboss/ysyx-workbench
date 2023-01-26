#include "../include/cpu/cpu.h"

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

int npc_state;

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


void eval_wave() {
  top->clock = !top->clock;
  top->io_inst = pmem_read(top->io_pc);
  top->eval();
#ifdef CONFIG_WAVE
  tfp->dump(main_time);
#endif
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

#ifdef CONFIG_WAVE
  init_wave();
#endif

  // initial i_cache
  init_cache(dir);

  top->clock = 1;
  top->reset = 1;

  // state is running
  npc_state = NPC_RUNNING;
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

void main_loop() {
  // Simulate until $finish
  while (!Verilated::gotFinish() && (main_time <= MAX_SIM_TIME) && (npc_state == NPC_RUNNING)) {
    if(main_time > 2){
      top->reset = 0;
    }

    eval_wave();
  }
}