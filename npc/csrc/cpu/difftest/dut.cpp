#include "cpu/cpu.h"
#include "cpu/difftest.h"
#include <dlfcn.h>

#ifdef CONFIG_DIFFTEST

extern const char *regs[];
extern const char *csrs[];
extern uint64_t *cpu_gpr;
extern uint64_t *cpu_csr;

static bool is_skip_ref = false;
void isa_reg_display(bool *);
uint8_t* guest_to_host(uint64_t paddr);
uint64_t host_to_guest(uint8_t *haddr);

void difftest_skip_ref() {
  is_skip_ref = true;
}

NEMUCPUState cpu_diff = {};

static void checkregs(NEMUCPUState *ref) {
  bool same = true;
  int list_len = 34 + CSR_NUM;
  bool err_list[list_len] = {};
  // check next pc
  if(ref->pc != cpu.pc) {
    log_write(ANSI_FMT("pc (next instruction) error: \n", ANSI_FG_RED));
    log_write("ref pc: 0x%016lx\n", ref->pc);
    log_write("dut pc: 0x%016lx\n", cpu.pc);
    same = false;
    err_list[33] = true;
  }

  // check reg
  for(int i = 0; i < 32; i++) {
    if(ref->gpr[i] != cpu_gpr[i]) {
      log_write(ANSI_FMT("reg[%d] %s error: \n", ANSI_FG_RED), i, regs[i]);
      log_write("ref %s: 0x%016lx\n", regs[i], ref->gpr[i]);
      log_write("dut %s: 0x%016lx\n", regs[i], cpu_gpr[i]);
      same = false;
      err_list[i] = true;
    }
  }

  // check csr
  for(int i = 0; i < CSR_NUM; i++) {
    if(ref->csr[i] != cpu_csr[i]) {
      log_write(ANSI_FMT("csr[%d] %s error: \n", ANSI_FG_RED), i, csrs[i]);
      log_write("ref %s: 0x%016lx\n", csrs[i], ref->csr[i]);
      log_write("dut %s: 0x%016lx\n", csrs[i], cpu_csr[i]);
      same = false;
      err_list[i+34] = true;
    }
  }

  if(!same) {
    // print all dut regs when error
    isa_reg_display(err_list);
    npc_state.state = NPC_ABORT;
    npc_state.halt_pc = cpu.pc;
  }
}

void init_difftest(char *ref_so_file, long img_size) {
  assert(ref_so_file != NULL);

  void *handle;
  handle = dlopen(ref_so_file, RTLD_LAZY);
  assert(handle);

  // for c++, type must be same
  ref_difftest_memcpy = (void(*)(uint64_t addr, void *buf, size_t n, bool direction)) dlsym(handle, "difftest_memcpy");
  assert(ref_difftest_memcpy);

  ref_difftest_regcpy = (void(*)(void *dut, bool direction)) dlsym(handle, "difftest_regcpy");
  assert(ref_difftest_regcpy);

  ref_difftest_exec = (void(*)(uint64_t n)) dlsym(handle, "difftest_exec");
  assert(ref_difftest_exec);

  ref_difftest_raise_intr =(void(*)(uint64_t NO)) dlsym(handle, "difftest_raise_intr");
  assert(ref_difftest_raise_intr);
 
  void (*ref_difftest_init)() = (void(*)())dlsym(handle, "difftest_init");
  assert(ref_difftest_init);

  log_write("Differential testing: %s\n", ANSI_FMT("ON", ANSI_FG_GREEN));
  log_write("The result of every instruction will be compared with %s.\n"
      "This will help you a lot for debugging, but also significantly reduce the performance. "
      "If it is not necessary, you can turn it off in menuconfig.\n", ref_so_file);

  ref_difftest_init();// must behind of memcpy img
  // copy img instruction to ref
  ref_difftest_memcpy(CONFIG_MBASE, guest_to_host(CONFIG_MBASE), img_size, DIFFTEST_TO_REF);
  ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
}

void difftest_step() {
  NEMUCPUState ref_r;

  if (is_skip_ref) {
    
    for (int i = 0; i < 32; i++) {
      cpu.gpr[i] = cpu_gpr[i];
    }

    for (int i = 0; i < CSR_NUM; i++) {
      cpu.csr[i] = cpu_csr[i];
    }

    
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }
  printf("%lx\n", cpu.pc);
  // ref execute once
  ref_difftest_exec(1);
  printf("%lx\n", cpu.pc);
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

  checkregs(&ref_r);
}

#endif
