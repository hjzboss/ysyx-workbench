#include "VGCD.h" // .v文件的名字为name，则为Vname.h
#include "verilated.h"
#include <stdio.h>
#include <stdlib.h>
#include "verilated_vcd_c.h" // 波形仿真

int main(int argc, char** argv, char** env) {
	VerilatedContext* contextp = new VerilatedContext;
	contextp->commandArgs(argc, argv);
	Vtop* top = new Vtop{contextp};
	// 生成波形
	VerilatedVcdC* tfp = new VerilatedVcdC;
	contextp->traceEverOn(true);
	top->trace(tfp, 99);
	tfp->open("./build/sim/obj_dir/wave.vcd");

	while (!contextp->gotFinish()) {
		int en = rand() & 0x1;
		int sw = rand() & 0xff;
		top->en = en;
		top->sw = sw;
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
