#include <cpu/cpu.h>
#define MAX_INST_TO_PRINT 10

// define placeholders for some property
#define __P_DEF_0  X,
#define __P_DEF_1  X,
#define __P_ONE_1  X,
#define __P_ZERO_0 X,
// define some selection functions based on the properties of BOOLEAN macro
#define MUXDEF(macro, X, Y)  MUX_MACRO_PROPERTY(__P_DEF_, macro, X, Y)
#define MUXNDEF(macro, X, Y) MUX_MACRO_PROPERTY(__P_DEF_, macro, Y, X)
#define MUXONE(macro, X, Y)  MUX_MACRO_PROPERTY(__P_ONE_, macro, X, Y)
#define MUXZERO(macro, X, Y) MUX_MACRO_PROPERTY(__P_ZERO_,macro, X, Y)

// test if a boolean macro is defined
#define ISDEF(macro) MUXDEF(macro, 1, 0)
// test if a boolean macro is undefined
#define ISNDEF(macro) MUXNDEF(macro, 1, 0)
// test if a boolean macro is defined to 1
#define ISONE(macro) MUXONE(macro, 1, 0)
// test if a boolean macro is defined to 0
#define ISZERO(macro) MUXZERO(macro, 1, 0)
// test if a macro of ANY type is defined
// NOTE1: it ONLY works inside a function, since it calls `strcmp()`
// NOTE2: macros defined to themselves (#define A A) will get wrong results
#define isdef(macro) (strcmp("" #macro, "" str(macro)) != 0)

// simplification for conditional compilation
#define __IGNORE(...)
#define __KEEP(...) __VA_ARGS__
// keep the code if a boolean macro is defined
#define IFDEF(macro, ...) MUXDEF(macro, __KEEP, __IGNORE)(__VA_ARGS__)
// keep the code if a boolean macro is undefined
#define IFNDEF(macro, ...) MUXNDEF(macro, __KEEP, __IGNORE)(__VA_ARGS__)
// keep the code if a boolean macro is defined to 1
#define IFONE(macro, ...) MUXONE(macro, __KEEP, __IGNORE)(__VA_ARGS__)
// keep the code if a boolean macro is defined to 0
#define IFZERO(macro, ...) MUXZERO(macro, __KEEP, __IGNORE)(__VA_ARGS__)

// Current simulation time (64-bit unsigned)
vluint64_t main_time = 0;

static VJzCore* top;
static VerilatedContext* contextp = NULL;
static VerilatedVcdC* tfp = NULL;
static bool g_print_step = false;
static uint64_t g_timer = 0; // unit: us
uint64_t g_nr_guest_inst = 0;
IFDEF(CONFIG_ITRACE, char logbuf[128]);

static uint8_t i_cache[65535] = {};

NPCState npc_state = { .state = NPC_STOP };

uint64_t get_time();

// Called by $time in Verilog
double sc_time_stamp () {
  return main_time; // Note does conversion to real, to match SystemC
}

uint8_t* guest_to_host(uint64_t paddr) { return i_cache + paddr - CONFIG_MBASE; }
uint64_t host_to_guest(uint8_t *haddr) { return haddr - i_cache + CONFIG_MBASE; }

static inline uint64_t host_read(void *addr, int len) {
  switch (len) {
    case 1: return *(uint8_t  *)addr;
    case 2: return *(uint16_t *)addr;
    case 4: return *(uint32_t *)addr;
    case 8: return *(uint64_t *)addr;
    default: return 0;
  }
}

uint64_t pmem_read(uint64_t addr, int len) {
  uint64_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

/*
static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}
*/

// todo
static void trace_and_difftest(uint64_t dnpc) {
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND) { log_write("%s\n", logbuf); }
#endif
  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(logbuf)); }
  //IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));
	// watchpoint
	//IFDEF(CONFIG_WATCHPOINT, scan_watchpoint(_this));
}

// for ebreak instruction
extern "C" void c_break() {
  npc_state.state = NPC_END;
  npc_state.halt_pc = top->io_pc;
}

static void load_img(char *dir) {
  FILE *fp = fopen(dir, "rb");

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(i_cache, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
}


void reset(int time) {
  top->reset = 1;
  while (time > 0) {
    top->clock = !top->clock;
    top->eval();
#ifdef CONFIG_WAVE
    tfp->dump(main_time);
#endif
    main_time++;
    time--;  
  }
  top->reset = 0;
}


static void eval_wave() {
  top->clock = !top->clock;
  top->io_inst = pmem_read(top->io_pc, 4);
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
  load_img(dir);

  top->clock = 0;
  reset(3);

  // state is running
  npc_state.state = NPC_RUNNING;
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

static void cpu_exec_once() {
  uint64_t pc = top->io_pc;
  uint32_t inst_val = pmem_read(pc, 4);
  eval_wave();
#ifdef CONFIG_ITRACE
  char *p = logbuf;
  p += snprintf(p, sizeof(logbuf), FMT_WORD ":", pc);
  int ilen = 4;
  int i;
  uint8_t *inst = (uint8_t *)&inst_val;
  for (i = ilen - 1; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  p += 1;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, logbuf + sizeof(logbuf) - p, pc, (uint8_t *)&inst_val, ilen);
#endif
}

void execute(uint64_t n) {
  while (n--) {
    // todo
    //if (Verilated::gotFinish() || (main_time > MAX_SIM_TIME)) npc_state.state = NPC_QUIT;
    cpu_exec_once();
    g_nr_guest_inst ++;
    trace_and_difftest(top->io_pc);
    if (npc_state.state != NPC_RUNNING) break;
    //IFDEF(CONFIG_DEVICE, device_update());

    if (npc_state.state != NPC_RUNNING) break;
    else {
      cpu_exec_once();
    }
  }
}

// todo
static void statistic() {
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  printf("host time spent = " NUMBERIC_FMT " us", g_timer);
  printf("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) printf("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else printf("Finish running in less than 1 us and can not calculate the simulation frequency");
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) {
  g_print_step = (n < MAX_INST_TO_PRINT);
  switch (npc_state.state) {
    case NPC_END: case NPC_ABORT:
      printf("Program execution has ended. To restart the program, exit NPC and run again.\n");
      return;
    default: npc_state.state = NPC_RUNNING;
  }

  uint64_t timer_start = get_time();

  execute(n);

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;

  // todo
  switch (npc_state.state) {
    case NPC_RUNNING: npc_state.state = NPC_STOP; break;

    case NPC_END: case NPC_ABORT:
      printf("npc: %s at pc = " FMT_WORD,
          (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          npc_state.halt_pc);
      // fall through
    case NPC_QUIT: statistic();
  }
}
