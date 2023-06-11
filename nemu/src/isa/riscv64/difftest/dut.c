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
#include <cpu/difftest.h>
#include "../local-include/reg.h"

void isa_reg_display_error(bool *err_list);

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  bool same = true;
  bool err_list[33] = {};
  // check next pc
  if(ref_r->pc != pc) {
    Log(ANSI_FMT("pc (next instruction) error: \n", ANSI_FG_RED));
    Log("ref pc: 0x%016lx\n", ref_r->pc);
    Log("dut pc: 0x%016lx\n", pc);
    same = false;
    err_list[32] = true;
  }

  // check reg
  for(int i = 0; i < 32; i++) {
    if(ref_r->gpr[i] != cpu.gpr[i]) {
      Log(ANSI_FMT("reg[%d] %s error: \n", ANSI_FG_RED), i, reg_name(i, 0));
      Log("ref %s: 0x%016lx\n", reg_name(i, 0), ref_r->gpr[i]);
      Log("dut %s: 0x%016lx\n", reg_name(i, 0), cpu.gpr[i]);
      same = false;
      err_list[i] = true;
    }
  }

  if(!same) {
    // print all dut regs when error
    isa_reg_display_error(err_list);
  }
  return same;
}

void isa_difftest_attach() {
}
