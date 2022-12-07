#include "Vtop.h" // .v文件的名字为name，则为Vname.h
#include <stdio.h>
#include <stdlib.h>
#include <nvboard.h> // nvboard

static TOP_NAME dut;

void nvboard_bind_all_pins(Vtop* top); 

static void single_cycle() {
	dut.clk = 0;
	dut.eval();
	dut.clk = 1;
	dut.eval();
}

static void reset(int n) {
	dut.rst = 1;
	while (n--)
		single_cycle();
	dut.rst = 0;
}

int main(int argc, char** argv, char** env) {
	nvboard_bind_all_pins(&dut);
	nvboard_init();
	
	//reset(10);

	while (1) {
		nvboard_update();
		//single_cycle();
	}

	nvboard_quit();
	return 0;
}
