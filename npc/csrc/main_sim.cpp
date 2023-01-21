#include "VJzCore.h" // .v文件的名字为name，则为Vname.h
#include "verilated.h"
#include <stdio.h>
#include <stdlib.h>
#include "verilated_vcd_c.h" // 波形仿真

uint32_t instr_cache[65535] = {};

void init_cache() {
  instr_cache[0] = 0x00138393;
  instr_cache[1] = 0x00238393;
  instr_cache[2] = 0x00338393;
}

uint32_t pmem_read(uint64_t pc) {
  return instr_cache[pc];
}


int main(int argc, char** argv, char** env) {
  VerilatedContext* contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);
  VJzCore* top = new VJzCore{contextp};
  // 生成波形
  VerilatedVcdC* tfp = new VerilatedVcdC;
  contextp->traceEverOn(true);
  top->trace(tfp, 99);
  tfp->open("./build/sim/obj_dir/wave.vcd");
  
  init_cache();
  while (contextp->time() < 3 && !contextp->gotFinish()) {
    top->io_inst = pmem_read(top->io_pc);
    top->eval();
    //printf("en = %o, sw = %o\n", top->en, top->sw, top->valid, top->led, top->seg0); 
    // 推进仿真时间
    tfp->dump(contextp->time());
    contextp->timeInc(1);
  }

  delete top;
  tfp->close();
  delete contextp;
  return 0;
}
