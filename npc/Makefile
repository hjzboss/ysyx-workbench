include ./scripts/sim-npc.mk

#NXDC_FILES = constr/top.nxdc

#FPGA_CSRC = $(shell find $(abspath ./csrc) -name "*_fpga.cpp")
#FPGA_CSRC += $(SRC_AUTO_BIND)

VERILATOR_FPGAFLAG = -Wall --cc \
										 -MMD --build --Mdir $(FPGA_OBJ_DIR) 

SRC_AUTO_BIND = $(abspath $(NPC_BUILD_DIR)/auto_bind.cpp)

#FPGA_OBJ_DIR = $(NPC_BUILD_DIR)/fpga/obj_dir
#BIN = $(FPGA_OBJ_DIR)/$(TOPNAME)

#传递给preproject连接器的信息，添加SDL2库信息
LDFLAGS += -lSDL2 -lSDL2_image

#包含文件路径，加前缀-I传递给g++，INC_PATH在nvbroad.mk中定义
INCFLAGS = $(addprefix -I, $(INC_PATH))

#传递给g++的编译参数，包括包含路径和TOP_NAME的定义
CFLAGS += $(INCFLAGS)  -DTOP_NAME="\"V$(TOPNAME)\""

#include $(NVBOARD_HOME)/scripts/nvboard.mk

export PATH := $(PATH):$(abspath ./utils)

# 单元测试有bug
#test:
#	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(NPC_BUILD_DIR)/sim
	mill -i __.runMain Elaborate -td $(NPC_BUILD_DIR)
#	mv $(NPC_BUILD_DIR)/${TOPNAME}.v $(NPC_BUILD_DIR)/${USER_ID}_${TOPNAME}.v

help:
	mill -i __.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(NPC_BUILD_DIR)

.PHONY: test verilog help compile bsp reformat checkformat clean

#fpga: $(BIN)
#	@$^

#$(BIN): $(NVBOARD_ARCHIVE) $(FPGA_CSRC) $(VSRC)
#	$(call git_commit, "sim RTL")
#	@rm -rf $(FPGA_OBJ_DIR)
#	$(VERILATOR) $(VERILATOR_FPGAFLAG) $^ \
		$(addprefix -CFLAGS , $(CFLAGS)) \
	 	$(addprefix -LDFLAGS , $(LDFLAGS)) \
		-top $(TOPNAME) \
		--exe -o $(abspath $(BIN))

# 将引脚映射转为cpp
#$(SRC_AUTO_BIND): $(NXDC_FILES)
#	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

#$(shell mkdir -p $(SIM_OBJ_DIR) $(FPGA_OBJ_DIR))

include ../Makefile
