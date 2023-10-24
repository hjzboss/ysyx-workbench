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
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>

#define REG_SIZE (264 + (8 * CSR_NUM) + 4)

void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  if(direction == DIFFTEST_TO_REF) {
    memcpy(guest_to_host(addr), buf, n);
  }
  else {
    memcpy(buf, guest_to_host(addr), n);
  }
}

void difftest_regcpy(void *dut, bool direction) {
  if(direction == DIFFTEST_TO_REF) {
    CPU_state tmp;
    memcpy(&tmp, dut, REG_SIZE);
    for (int i = 0; i < 32; i++) {
      if(cpu.gpr[i] != tmp.gpr[i]) {
        printf("%d diff: dut=%lx, ref=%lx, pc=%lx\n", i, tmp.gpr[i], cpu.gpr[i], tmp.pc);
      }
    }
    for (int i = 0; i < CSR_NUM; i++) {
      if(cpu.csr[i] != tmp.csr[i]) {
        printf("csr[%d] diff: dut=%lx, ref=%lx, pc=%lx\n", i, tmp.csr[i], cpu.csr[i], tmp.pc);
      }
    }
    memcpy(&cpu, dut, REG_SIZE);
  }
  else {
    memcpy(dut, &cpu, REG_SIZE);
  }
}

void difftest_exec(uint64_t n) {
  cpu_exec(n);
}

void difftest_raise_intr(word_t NO) {
  assert(0);
}

void difftest_init(int port) {
  /* Perform ISA dependent initialization. */
  init_isa();
}
