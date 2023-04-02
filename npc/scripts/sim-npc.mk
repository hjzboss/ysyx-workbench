BUILD_DIR = ${NPC_HOME}/build

BLACKBOX_DIR = ${NPC_HOME}/core/src/main/verilog

TOPNAME = JzCore

VSRC = $(shell find $(abspath ${BUILD_DIR}) -name "*.v")
VSRC += $(shell find $(abspath ${BLACKBOX_DIR}) -name "*.sv")

SIM_CSRC = $(shell find $(abspath ${NPC_HOME}/csrc) -name "*.cpp")

VERILATOR = verilator
SIM_OBJ_DIR = $(BUILD_DIR)/sim/obj_dir
WAVE = wave.vcd

VERILATOR_SIMFLAG = 
# build
VERILATOR_SIMFLAG += --cc --exe --build -MMD -j 0
# C++ compiler arguments for makefile
VERILATOR_SIMFLAG += -CFLAGS "-I${NPC_HOME}/include -O2 -I/usr/lib/llvm-14/include -std=c++14 -fno-exceptions -D_GNU_SOURCE -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS"
# open trace
VERILATOR_SIMFLAG += --trace --Mdir $(SIM_OBJ_DIR)
# top module
VERILATOR_SIMFLAG += --top-module $(TOPNAME)

IMAGE_OBJ ?= 

DIFFSET_SO := ${NEMU_HOME}/build/riscv64-nemu-interpreter-so

NPC_FLAG += -l $(BUILD_DIR)/npc-log.txt
NPC_FLAG += -i $(IMAGE_OBJ)
NPC_FLAG += -e ${IMAGE}.elf
NPC_FLAG += -d ${DIFFSET_SO}

LFLAGS += $(shell llvm-config --libs) -lreadline -ldl -pie -lSDL2
LFLAGS += $(DIFFSET_SO)

VERILATOR_SIMFLAG += -LDFLAGS "$(LFLAGS)"

sim: $(SIM_CSRC) $(VSRC)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(SIM_OBJ_DIR)
	@echo "build"
	$(VERILATOR) $(VERILATOR_SIMFLAG) $^
	$(SIM_OBJ_DIR)/V$(TOPNAME) $(NPC_FLAG)
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE)