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

#include <isa.h>
#include "local-include/reg.h"

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

const char *csrs[] = {
  "mstatus", "mtvec", "mepc", "mcause"
};

void isa_reg_display() {
  int i;
  printf("------------------------------\n");
  printf("reg \tvalue\n");
  printf("------------------------------\n");
  printf("pc:\t0x%016lx\n", cpu.pc);
  for (i=0; i<32; ++i) {
    printf("%s:\t0x%016lx\n", regs[i], cpu.gpr[i]);
  }
  for (i=0; i < CSR_NUM; ++i) {
    printf("%s:\t0x%016lx\n", csrs[i], cpu.csr[i]);
  }
  printf("------------------------------\n");
}

void isa_reg_display_error(bool *err_list) {
  int i;
  printf(ANSI_FMT("CPU register state: \n", ANSI_FG_BLUE));
  printf("------------------------------\n");
  printf("reg \tvalue\n");
  printf("------------------------------\n");
  if (err_list[32]) printf(ANSI_FMT("pc:\t0x%016lx\n", ANSI_FG_RED), cpu.pc);
  else printf("pc:\t0x%016lx\n", cpu.pc);

  for (i=0; i<32; ++i) {
    if (err_list[i]) printf(ANSI_FMT("%s:\t0x%016lx\n", ANSI_FG_RED), regs[i], cpu.gpr[i]);
    else printf("%s:\t0x%016lx\n", regs[i], cpu.gpr[i]);
  }
  printf("------------------------------\n");
}


word_t isa_reg_str2val(const char *s, bool *success) {
  for (int i = 0; i < 32; i ++) {
    if (strcmp(s, regs[i]) == 0) {
      *success = true;
      return cpu.gpr[i];
    }
  }

  for (int i = 0; i < CSR_NUM; i++) {
    if (strcmp(s, csrs[i]) == 0) {
      *success = true;
      return cpu.csr[i];
    }
  }

  *success = false;
  return 0;
}
