#include "Vtop.h" // .v文件的名字为name，则为Vname.h
#include <stdio.h>
#include <stdlib.h>
#include <nvboard.h> // nvboard

static TOP_NAME dut;

void nvboard_bind_all_pins(Vtop* top); 

static void single_cycle() {
	dut.clock = 0;
	dut.eval();
	unsigned long n = 1844674407370955161;
	for (unsigned long i = n; i > 0; --i) {
		for (unsigned long j = n; j > 0; --j) {
			for (unsigned long k = n; k > 0; --k) {
				for (unsigned long w = n; w > 0; --w) {}
			}
		}
	}
	dut.clock = 1;
	dut.eval();
	for (unsigned long i = n; i > 0; --i) {
		for (unsigned long j = n; j > 0; --j) {
			for (unsigned long k = n; k > 0; --k) {
				for (unsigned long w = n; w > 0; --w) {}
			}
		}
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
