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
VERILATOR_SIMFLAG += --cc --exe --build -MMD
# C++ compiler arguments for makefile
VERILATOR_SIMFLAG += -CFLAGS "-I${NPC_HOME}/include"
# open trace
VERILATOR_SIMFLAG += --trace --Mdir $(SIM_OBJ_DIR)
# top module
VERILATOR_SIMFLAG += --top-module $(TOPNAME)

LFLAGS += $(shell llvm-config --libs) -lreadline -ldl -pie -lSDL2
#LFLAGS +=  -lreadline -ldl -pie -lSDL2
#LDFLAGS += $(DIFFSET_SO)

VERILATOR_SIMFLAG += -LDFLAGS "$(LFLAGS)"

IMAGE_OBJ ?= 

sim: $(SIM_CSRC) $(VSRC)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(SIM_OBJ_DIR)
	@echo "build"
	$(VERILATOR) $(VERILATOR_SIMFLAG) $^
	$(SIM_OBJ_DIR)/V$(TOPNAME) $(IMAGE_OBJ)
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE)