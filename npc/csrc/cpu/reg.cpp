#include "verilated_dpi.h"
#include <cpu/cpu.h>

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu.gpr = (uint64_t *)(((VerilatedDpiOpenVar*)r)->datap());
}

void isa_reg_display(bool *err_list) {
  int i;
  printf(ANSI_FMT("CPU register state: \n", ANSI_FG_BLUE));
  printf("------------------------------\n");
  printf("reg \tvalue\n");
  printf("------------------------------\n");
  printf("pc:\t0x%016lx\n", cpu.pc);
  #ifdef CONFIG_DIFFTEST
  if (err_list[33]) printf(ANSI_FMT("%s:\t0x%016lx\n", ANSI_FG_RED), regs[i], cpu.gpr[i]);
  else printf("%s:\t0x%016lx\n", regs[i], cpu.gpr[i]);
  #endif

  for (i=0; i<32; ++i) {
    if (err_list[i]) printf(ANSI_FMT("%s:\t0x%016lx\n", ANSI_FG_RED), regs[i], cpu.gpr[i]);
    else printf("%s:\t0x%016lx\n", regs[i], cpu.gpr[i]);
  }
  printf("------------------------------\n");
}

uint64_t isa_reg_str2val(const char *s, bool *success) {
  for (int i = 0; i < 32; i ++) {
    if (strcmp(s, regs[i]) == 0) {
      *success = true;
      return cpu.gpr[i];
    }
  }

  *success = false;
  return 0;
}
