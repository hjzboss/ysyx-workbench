#include "Vtop.h" // .v文件的名字为name，则为Vname.h
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
		int x = rand() & 0xf;
		int y = rand() & 0x3;
		top->x = x;
		top->y = y;
		top->eval();
		printf("x = %o, y = %o, f = %o\n", x, y, top->f);
		// 推进仿真时间
		tfp->dump(contextp->time());
		contextp->timeInc(1);
	}
	delete top;
	tfp->close();
	delete contextp;
	return 0;
}
