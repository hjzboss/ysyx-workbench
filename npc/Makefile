v_name = top
c_name = main

all:
	@echo "Write this Makefile by your self."

build:
	verilator --cc --exe --build -Wall --trace ./csrc/$(c_name).cpp ./vsrc/$(v_name).v
	./obj_dir/V$(v_name)

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by your self."
	gtkwave ./obj_dir/wave.vcd

include ../Makefile

.PHONY: clean

clean:
	rm -r obj_dir
