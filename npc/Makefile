TOPNAME = top
NXDC_FILES = constr/top.nxdc

VSRC = $(shell find $(abspath ./vsrc) -name "*.v")
FPGA_CSRC = $(shell find $(abspath ./csrc) -name "*_fpga.cpp")
FPGA_CSRC += $(SRC_AUTO_BIND)
SIM_CSRC = $(shell find $(abspath ./csrc) -name "*_sim.cpp")

VERILATOR = verilator
VERILATOR_SIMFLAG = --cc --exe --build -Wall --trace --Mdir $(SIM_OBJ_DIR) --top-module $(TOPNAME)
VERILATOR_FPGAFLAG = --exe -Wall --cc -MMD --build --Mdir $(FPGA_OBJ_DIR) --top-module $(TOPNAME)

INC_PATH ?= 
INCFLAGS = $(addprefix -I, $(INC_PATH))
CFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""
LDFLAGS += -lSDL2 -lSDL2_image

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)

BUILD_DIR = ./build
SIM_OBJ_DIR = $(BUILD_DIR)/sim/obj_dir
FPGA_OBJ_DIR = $(BUILD_DIR)/fpga/obj_dir
WAVE = wave.vcd

all:
	@echo "Write this Makefile by your self."

b: $(SIM_CSRC) $(VSRC)
	@echo "build"
	@rm -rf $(SIM_OBJ_DIR)
	$(VERILATOR) $(VERILATOR_SIMFLAG) $^
	$(SIM_OBJ_DIR)/V$(TOPNAME)

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by your self."
	@rm -rf $(SIM_OBJ_DIR)
	gtkwave $(SIM_OBJ_DIR)/$(WAVE)

fpga: $(FPGA_CSRC) $(VSRC) $(NVBOARD_ARCHIVE)
	$(call git_commit, "sim RTL")
	@echo "run nvboard"
	@rm -rf $(FPGA_OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FPGAFLAG) $^ \
		$(addprefix -CFLAGS , $(CFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS))

# 将引脚映射转为cpp
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

include $(NVBOARD_HOME)/scripts/nvboard.mk
include ../Makefile

.PHONY: clean

clean:
	rm -r obj_dir
