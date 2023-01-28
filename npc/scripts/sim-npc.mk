BUILD_DIR = ${NPC_HOME}/build

BLACKBOX_DIR = ${NPC_HOME}/core/src/main/verilog

TOPNAME = JzCore

VSRC = $(shell find $(abspath ${BUILD_DIR}) -name "*.v")
VSRC += $(shell find $(abspath ${BLACKBOX_DIR}) -name "*.sv")

SIM_CSRC = $(shell find $(abspath ${NPC_HOME}/csrc) -name "*.cpp")

VERILATOR = verilator
VERILATOR_SIMFLAG = --cc -I ${NPC_HOME}/include --exe --build --trace --Mdir $(SIM_OBJ_DIR) --top-module $(TOPNAME)

SIM_OBJ_DIR = $(BUILD_DIR)/sim/obj_dir
WAVE = wave.vcd

IMAGE_OBJ ?= 

sim: $(SIM_CSRC) $(VSRC)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(SIM_OBJ_DIR)
	@echo "build"
	$(VERILATOR) $(VERILATOR_SIMFLAG) $^
	$(SIM_OBJ_DIR)/V$(TOPNAME) $(IMAGE_OBJ)
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE)