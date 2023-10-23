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

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */
  word_t mstatus = cpu.csr[0];
  word_t mie = mstatus & 0x008;
  word_t mpie = mie << 4;
  mstatus &= ~0x088; // clear mpie and mie
  mstatus |= mpie; // set mpie=mie and mie=0
  cpu.csr[0] = mstatus;
  cpu.csr[3] = 0xb; // mcause syscall
  cpu.csr[2] = epc;  // mepc
  printf("nemu: ecall, mstatus=%lx\n", mstatus);
  return cpu.csr[1]; // mtvec
}

word_t isa_mret() {
  // set mstatus and return epc
  word_t mstatus = cpu.csr[0];
  word_t mpie = mstatus & 0x080;
  word_t mie = mpie >> 4;
  mstatus &= ~0x088; // clear mpie and mie
  mstatus |= (mie | 0x080); // set mpie=1 and mie=mpie
  cpu.csr[0] = mstatus;
  printf("nemu: mret, mstatus=%lx\n", mstatus);
  return cpu.csr[2];
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
