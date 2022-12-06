#include "Vtop.h" // .v文件的名字为name，则为Vname.h
#include <stdio.h>
#include <stdlib.h>
#include <nvboard.h> // nvboard

static TOP_NAME dut;

void nvboard_bind_all_pins(Vtop* top); 

int main(int argc, char** argv, char** env) {
	nvboard_bind_all_pins(&dut);
	nvboard_init();

	while (1) {
		dut.eval();
		nvboard_update();
	}

	nvboard_quit();
	return 0;
}
