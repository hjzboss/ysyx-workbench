#include <cpu/cpu.h>
#include <time.h>
#include <sys/time.h>
#include <stdlib.h>
#define MAX_INST_TO_PRINT 10

// Current simulation time (64-bit unsigned)
vluint64_t main_time = 0;

static VJzCore* top;
static VerilatedContext* contextp = NULL;
static VerilatedVcdC* tfp = NULL;
static bool g_print_step = false;
static uint64_t g_timer = 0; // unit: us
uint64_t g_nr_guest_inst = 0;
extern uint64_t* gpr;
static struct timeval boot_time = {};

CPUState npc_cpu = {};

IFDEF(CONFIG_MTRACE, void free_mtrace());
IFDEF(CONFIG_DTRACE, void free_dtrace());
#ifdef CONFIG_DIFFTEST
void difftest_skip_ref();
void difftest_step();
#endif

// itrace iringbuf
#ifdef CONFIG_ITRACE
static char* iringbuf[MAX_INST_TO_PRINT] = {};
static int iring_ptr = -1;

void init_iringbuf() {
  for (int i = 0; i < MAX_INST_TO_PRINT; ++i) {
    iringbuf[i] = (char*)malloc(128);
  }
}

static void insert_iringbuf() {
  iring_ptr = (iring_ptr + 1) % MAX_INST_TO_PRINT;
  strcpy(iringbuf[iring_ptr], npc_cpu.logbuf);
}

static void print_iringbuf() {
  printf("---itrace message start---\n");
  for (int i = 0; i < MAX_INST_TO_PRINT; ++i) {
    if (iring_ptr == i) printf("-->");
    else printf("   ");

    puts(iringbuf[i]);
  }
  printf("---itrace message end---\n");
}
#endif

#ifdef CONFIG_FTRACE
void free_ftrace();
void print_ftrace(bool);
void ftrace(uint64_t addr, uint32_t inst, uint64_t next_pc);
#endif

NPCState npc_state = { .state = NPC_STOP };

uint64_t get_time();

uint64_t paddr_read(uint64_t addr, int len);
void paddr_write(uint64_t addr, int len, uint64_t data);
long load_img(char *dir);
void isa_reg_display(bool*);
void print_mtrace();

// Called by $time in Verilog
double sc_time_stamp () {
  return main_time; // Note does conversion to real, to match SystemC
}


// todo: watchpoint
static void trace_and_difftest() {
  IFDEF(CONFIG_ITRACE, log_write("%s\n", npc_cpu.logbuf));
  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(npc_cpu.logbuf)); }
  IFDEF(CONFIG_DIFFTEST, difftest_step());
	// watchpoint
	//IFDEF(CONFIG_WATCHPOINT, scan_watchpoint(_this));
}

// for ebreak instruction
extern "C" void c_break(long long halt_ret) {
  npc_state.state = NPC_END;
  npc_state.halt_pc = top->io_pc;
  npc_state.halt_ret = halt_ret;
}

extern "C" void inst_read(long long raddr, int *rdata) {
  if (raddr < 0x80000000ull) {
    *rdata = 0x00000013;
    return;
  }
  *rdata = paddr_read(raddr, 4);
}

extern "C" void pmem_read(long long raddr, long long *rdata) {
  // 总是读取地址为`raddr & ~0x7ull`的8字节返回给`rdata`
  if (raddr < 0x80000000ull) {
    *rdata = 0x00000000;
    return;
  }
  else if (raddr == CONFIG_TIMER_MMIO || raddr == CONFIG_TIMER_MMIO + 8) {
    // timer
    if (raddr == CONFIG_TIMER_MMIO + 8) {
      gettimeofday(&boot_time, NULL);
    }
    else if (raddr == CONFIG_TIMER_MMIO) {
      struct timeval now;
      gettimeofday(&now, NULL);
      long seconds = now.tv_sec - boot_time.tv_sec;
      long useconds = now.tv_usec - boot_time.tv_usec;
      *rdata = seconds * 1000000;
    }
    return;
  }
  else {
    *rdata = paddr_read(raddr & ~0x7ull, 8);
  }
}


extern "C" void pmem_write(long long waddr, long long wdata, char wmask) {
  // 总是往地址为`waddr & ~0x7ull`的8字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  if (wmask == 0 || waddr < 0x80000000ull) return;
  if (waddr == CONFIG_SERIAL_MMIO) {
    // uart
    putchar(wdata);
    return;
  }
  else {
    uint64_t rdata = paddr_read(waddr & ~0x7ull, 8);
    uint64_t wmask_64 = 0;
    uint8_t *index = (uint8_t*)&wmask_64;
    // 将8位的掩码转换为64位的掩码
    for(int i = 0; i < 8; i++, index++) {
      if(wmask & 0x01 == 0x01) {
        *index = 0xff;
      }
      wmask = wmask >> 1;
    }
    // 需要将要写入的数据进行移位，移位到掩码为1的部分，跳过右侧的0
    uint64_t tmp = wmask_64;
    int shift_cnt = 0;
    for(int i = 64; i > 0; i--) {
      if(tmp & 0x01 == 0x01) break;
      shift_cnt++;
      tmp = tmp >> 1;
    }
    rdata = (rdata & ~wmask_64) + ((wdata << shift_cnt) & wmask_64);
    paddr_write(waddr & ~0x7ull, 8, rdata);
  }
}


static void reset(int time) {
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


long init_cpu(char *dir) {
  // Construct the Verilated model, from Vjzcore.h generated from Verilating "jzcore.v"
  top = new VJzCore; // Or use a const unique_ptr, or the VL_UNIQUE_PTR wrapper

  IFDEF(CONFIG_WAVE, init_wave());

  // initial i_cache
  long size = load_img(dir);

  top->clock = 0;
  reset(4);

  npc_cpu.pc = top->io_pc;
  npc_cpu.npc = top->io_nextPc;

  // state is running
  npc_state.state = NPC_RUNNING;

  // initial mstatus
  IFDEF(CONFIG_DIFFTEST, npc_cpu.csr[0] = 0xa00001800);

  return size;
}


void delete_cpu() {
  // Final model cleanup
  top->final();

  // Close trace if opened
  if (tfp) { tfp->close(); }

  // Destroy model
  delete top; 
  top = NULL;

  IFDEF(CONFIG_FTRACE, free_ftrace());
  IFDEF(CONFIG_MTRACE, free_mtrace());
  IFDEF(CONFIG_DTRACE, free_dtrace());
}

static void isa_exec_once() {
  eval_wave();
  eval_wave();
}

static void cpu_exec_once() {
  uint64_t pc = top->io_pc;
  npc_cpu.npc = top->io_nextPc;
  npc_cpu.inst = paddr_read(npc_cpu.pc, 4);
  isa_exec_once();
  npc_cpu.pc = top->io_pc;
#ifdef CONFIG_ITRACE
  char *p = npc_cpu.logbuf;
  p += snprintf(p, sizeof(npc_cpu.logbuf), FMT_WORD ":", pc);
  int ilen = 4;
  int i;
  uint8_t *inst = (uint8_t *)&npc_cpu.inst;
  for (i = ilen - 1; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  int ilen_max = 4;
  int space_len = ilen_max - ilen;
  space_len = space_len * 3 + 1;
  memset(p, ' ', space_len);
  p += space_len;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, npc_cpu.logbuf + sizeof(npc_cpu.logbuf) - p, pc, (uint8_t *)&npc_cpu.inst, ilen);

  insert_iringbuf();
#endif

#ifdef CONFIG_FTRACE
  ftrace(pc, npc_cpu.inst, npc_cpu.npc);
#endif
}

void execute(uint64_t n) {
  while (n--) {
    //if (Verilated::gotFinish() || (main_time > MAX_SIM_TIME)) npc_state.state = NPC_QUIT;
    cpu_exec_once();
    g_nr_guest_inst ++;
    trace_and_difftest();
    if (npc_state.state != NPC_RUNNING) break;
    //IFDEF(CONFIG_DEVICE, device_update());
  }
}

// todo
static void statistic() {
  IFDEF(CONFIG_FTRACE, print_ftrace(true));
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  printf("host time spent = " NUMBERIC_FMT " us", g_timer);
  printf("\ntotal guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) printf("\nsimulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else printf("\nFinish running in less than 1 us and can not calculate the simulation frequency\n");
}

void assert_fail_msg() {
  IFDEF(CONFIG_ITRACE, print_iringbuf());
  IFDEF(CONFIG_MTRACE, print_mtrace());
  //IFDEF(CONFIG_FTRACE, print_ftrace(false));
  bool err_list[34];
  isa_reg_display(err_list);
  statistic();
}

/* Simulate how the npc_cpu works. */
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
    case NPC_RUNNING: /*npc_state.state = NPC_STOP;*/ break;

    case NPC_END: case NPC_ABORT:
      printf("npc: %s at pc = " FMT_WORD,
          (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          npc_state.halt_pc);
          printf("\n");
    // fall through
    case NPC_QUIT: statistic();
  }
}
