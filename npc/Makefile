TOPNAME = top
NXDC_FILES = constr/top.nxdc

VSRC = $(shell find $(abspath ./vsrc) -name "*.v")
FPGA_CSRC = $(shell find $(abspath ./csrc) -name "*_fpga.cpp")
FPGA_CSRC += $(SRC_AUTO_BIND)
SIM_CSRC = $(shell find $(abspath ./csrc) -name "*_sim.cpp")

VERILATOR = verilator
VERILATOR_SIMFLAG = --cc --exe --build -Wall --trace --Mdir $(SIM_OBJ_DIR) --top-module $(TOPNAME)
VERILATOR_FPGAFLAG = -Wall --cc \
										 -MMD --build --Mdir $(FPGA_OBJ_DIR) 

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)

BUILD_DIR = ./build
SIM_OBJ_DIR = $(BUILD_DIR)/sim/obj_dir
FPGA_OBJ_DIR = $(BUILD_DIR)/fpga/obj_dir
BIN = $(FPGA_OBJ_DIR)/$(TOPNAME)
WAVE = wave.vcd

#传递给preproject连接器的信息，添加SDL2库信息
LDFLAGS += -lSDL2 -lSDL2_image

#包含文件路径，加前缀-I传递给g++，INC_PATH在nvbroad.mk中定义
INCFLAGS = $(addprefix -I, $(INC_PATH))

#传递给g++的编译参数，包括包含路径和TOP_NAME的定义
CFLAGS += $(INCFLAGS)  -DTOP_NAME="\"V$(TOPNAME)\""

all:
	@echo "Write this Makefile by your self."

run: $(BIN)
	@echo $(INC_PATH)
	$@^

b: $(SIM_CSRC) $(VSRC)
	@rm -rf $(SIM_OBJ_DIR)
	$(VERILATOR) $(VERILATOR_SIMFLAG) $^
	$(SIM_OBJ_DIR)/V$(TOPNAME)

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by your self."
	@echo $(INC_PATH)
	gtkwave $(SIM_OBJ_DIR)/$(WAVE)

$(BIN): $(FPGA_CSRC) $(VSRC) $(NVBOARD_ARCHIVE)
	$(call git_commit, "sim RTL")
	@echo "run nvboard"
	@rm -rf $(FPGA_OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FPGAFLAG) $^ \
		$(addprefix -CFLAGS , $(CFLAGS)) \
	 	$(addprefix -LDFLAGS , $(LDFLAGS)) \
		-top $(TOPNAME) \
		--exe -o $(abspath $(BIN))

# 将引脚映射转为cpp
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

$(shell mkdir -p $(SIM_OBJ_DIR) $(FPGA_OBJ_DIR))

include $(NVBOARD_HOME)/scripts/nvboard.mk
include ../Makefile

.PHONY: default all clean run

clean:
	rm -r $(BUILD_DIR)
