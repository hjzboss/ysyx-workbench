/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>
#include <string.h>

/* The assembly code of instructions executed is only output to the screen
 * when the number of instructions executed is less than this value.
 * This is useful when you use the `si' command.
 * You can modify this value as you want.
 */
#define MAX_INST_TO_PRINT 10

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0; // unit: us
static bool g_print_step = false;

void device_update();
bool scan_wp();
void scan_watchpoint(Decode*);
IFDEF(CONFIG_MTRACE, void print_mtrace());

// todo
#ifdef CONFIG_ETRACE1
typedef struct node {
  char cause[32];
  word_t csr[4];
  struct node *next;
} etrace_node;

static etrace_node *etrace_head = NULL;
static etrace_node *etrace_tail = NULL;

static void insert_etrace(char *cause) {
  etrace_node *node = (etrace_node*)malloc(sizeof(etrace_node));
  strcmp(node->cause, cause);
  memcpy(node->csr, cpu.csr, sizeof(word_t) * CSR_NUM);
  node->next = NULL;

  if (etrace_head == NULL) {
    etrace_head = node;
    etrace_tail = node;
  }
  else {
    etrace_tail->next = node;
    etrace_tail = node;
  }
}

void free_etrace() {
  etrace_node *tmp;
  while(etrace_head != NULL) {
    tmp = etrace_head->next;
    free(etrace_head);
    etrace_head = tmp;
  }
}

void print_etrace() {
  log_write("---etrace message start---\n");
  etrace_node *ptr = etrace_head;
  while(ptr != NULL) {
    log_write("excep instr:%s| mstatus=%lx, mtvec=%lx, mepc=%lx, mcause=%lx\n", ptr->cause, ptr->csr[0], ptr->csr[1], ptr->csr[2], ptr->csr[3]);
    ptr = ptr->next;
  }
  log_write("---etrace message end---\n");
}
#endif

#ifdef CONFIG_ITRACE
// my change
static char* iringbuf[MAX_INST_TO_PRINT] = {};
static int iring_ptr = -1;

void init_iringbuf() {
  for (int i = 0; i < MAX_INST_TO_PRINT; ++i) {
    iringbuf[i] = (char*)malloc(128);
  }
}

static void insert_iringbuf(char* logbuf) {
  iring_ptr = (iring_ptr + 1) % MAX_INST_TO_PRINT;
  strcpy(iringbuf[iring_ptr], logbuf);
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
void print_ftrace(bool);
void ftrace(paddr_t addr, uint32_t inst, paddr_t next_pc);
#endif

#ifdef CONFIG_DTRACE
void print_dtrace();
#endif

static void trace_and_difftest(Decode *_this, vaddr_t dnpc) {
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND) { log_write("%s\n", _this->logbuf); }
#endif
  log_write("%s\n", _this->logbuf);
  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
  //IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));
	// watchpoint
	IFDEF(CONFIG_WATCHPOINT, scan_watchpoint(_this));
}


static void exec_once(Decode *s, vaddr_t pc) {
  s->pc = pc;
  s->snpc = pc;
  isa_exec_once(s);
  cpu.pc = s->dnpc;
  cpu.inst = s->isa.inst.val;
  //printf("nemu_inst=%08x\n", cpu.inst);
#ifdef CONFIG_ITRACE
  char *p = s->logbuf;
  p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
  int ilen = s->snpc - s->pc;
  int i;
  uint8_t *inst = (uint8_t *)&s->isa.inst.val;
  for (i = ilen - 1; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  int ilen_max = MUXDEF(CONFIG_ISA_x86, 8, 4);
  int space_len = ilen_max - ilen;
  if (space_len < 0) space_len = 0;
  space_len = space_len * 3 + 1;
  memset(p, ' ', space_len);
  p += space_len;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, s->logbuf + sizeof(s->logbuf) - p,
      MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc), (uint8_t *)&s->isa.inst.val, ilen);
  
  insert_iringbuf(s->logbuf);
#endif

#ifdef CONFIG_FTRACE
  ftrace(pc, s->isa.inst.val, s->dnpc);
#endif
}

static void execute(uint64_t n) {
  Decode s;
  for (; n > 0; n --) {
    exec_once(&s, cpu.pc);
    g_nr_guest_inst ++;
    trace_and_difftest(&s, cpu.pc);
    if (nemu_state.state != NEMU_RUNNING) break;
    IFDEF(CONFIG_DEVICE, device_update());
  }
}

static void statistic() {
  IFDEF(CONFIG_FTRACE, print_ftrace(true));
  IFDEF(CONFIG_DTRACE, print_dtrace());
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64 
  Log("host time spent = " NUMBERIC_FMT " us", g_timer);
  Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void assert_fail_msg() {
  IFDEF(CONFIG_ITRACE, print_iringbuf());
  IFDEF(CONFIG_MTRACE, print_mtrace());
  IFDEF(CONFIG_FTRACE, print_ftrace(false));
  isa_reg_display();
  statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) {
  g_print_step = (n < MAX_INST_TO_PRINT);
  switch (nemu_state.state) {
    case NEMU_END: case NEMU_ABORT:
      printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
      return;
    default: nemu_state.state = NEMU_RUNNING;
  }

  uint64_t timer_start = get_time();

  execute(n);

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;

  switch (nemu_state.state) {
    case NEMU_RUNNING: nemu_state.state = NEMU_STOP; break;

    case NEMU_END: case NEMU_ABORT:
      Log("nemu: %s at pc = " FMT_WORD,
          (nemu_state.state == NEMU_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (nemu_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          nemu_state.halt_pc);
      // fall through
    case NEMU_QUIT: statistic();
  }
}
