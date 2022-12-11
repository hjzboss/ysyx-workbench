#include "Vtop.h" // .v文件的名字为name，则为Vname.h
#include <stdio.h>
#include <stdlib.h>
#include <nvboard.h> // nvboard

static TOP_NAME dut;

void nvboard_bind_all_pins(Vtop* top); 

static void single_cycle() {
	dut.clock = 0;
	dut.eval();
	int n = 100000;
	for (int i = n; i > 0; --i) {
		for (int j = n; j > 0; --j) {}
	}
	dut.clock = 1;
	dut.eval();
	for (int i = n; i > 0; --i) {
		for (int j = n; j > 0; --j) {}
	}
}

static void reset(int n) {
	dut.reset = 1;
	while (n--)
		single_cycle();
	dut.reset = 0;
}

int main(int argc, char** argv, char** env) {
	nvboard_bind_all_pins(&dut);
	nvboard_init();
	
	reset(10);

	while (1) {
		//dut.eval();
		nvboard_update();
		single_cycle();
	}

	nvboard_quit();
	return 0;
}
