# npc构建目录
NPC_BUILD_DIR = ${NPC_HOME}/build

# chisel blackbox代码目录
BLACKBOX_DIR = ${NPC_HOME}/core/src/main/verilog

# ysyx id
USER_ID = ysyx_22050853

# chisel生成的顶层文件名
TOPNAME = Soc

# verilog source
VSRC_NPC = ${NPC_BUILD_DIR}/${USER_ID}_${TOPNAME}.v
VSRC_NPC += $(shell find $(abspath ${BLACKBOX_DIR}) -name "*.sv")

# cpp source
SIM_CSRC_NPC = $(shell find $(abspath ${NPC_HOME}/csrc) -name "*.cpp")

# npc仿真文件目录
NPC_SIM_OBJ_DIR = $(NPC_BUILD_DIR)/sim/obj_dir

# 波形文件名称
WAVE_OBJ = wave.vcd

# 加载的镜像文件
IMAGE_OBJ ?= 

# npc仿真参数
# log文件地址
NPC_FLAG += -l $(NPC_BUILD_DIR)/npc-log.txt
# 所加载的镜像文件
NPC_FLAG += -i $(IMAGE_OBJ)
# elf文件，用于etrace
NPC_FLAG += -e ${IMAGE}.elf
# 批处理模式
NPC_FLAG += -b

# difftest动态库
DIFFSET_SO := ${NEMU_HOME}/build/riscv64-nemu-interpreter-so

# npc链接参数
LFLAGS_NPC += $(shell llvm-config --libs) -lreadline -ldl -pie -lSDL2

# npc c参数
CFLAGS_NPC = -I${NPC_HOME}/include -O2 -I/usr/lib/llvm-14/include -std=c++14 -fno-exceptions -D_GNU_SOURCE -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS

# difftest
ifneq ($(DIFF),)
NPC_FLAG += -d ${DIFFSET_SO}
LFLAGS_NPC += $(DIFFSET_SO)
CFLAGS_NPC += -DCONFIG_DIFFTEST=1
endif

# wave
ifneq ($(WAVE),)
CFLAGS_NPC += -DCONFIG_WAVE=1
endif

# verilator仿真参数
VERILATOR_SIMFLAG_NPC = 
# build
VERILATOR_SIMFLAG_NPC += --cc --exe --build -MMD
# C++ compiler arguments for makefile
VERILATOR_SIMFLAG_NPC += -CFLAGS "${CFLAGS_NPC}"
# open trace
VERILATOR_SIMFLAG_NPC += --trace --Mdir $(NPC_SIM_OBJ_DIR)
# top module
VERILATOR_SIMFLAG_NPC += --top-module ${USER_ID}_$(TOPNAME)
# 链接
VERILATOR_SIMFLAG_NPC += -LDFLAGS "$(LFLAGS_NPC)"

# 仿真
sim: 
#	@echo "generate verilog"
#	make -C ${NPC_HOME} verilog
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(NPC_SIM_OBJ_DIR)
	@echo "build npc verilator"
	verilator $(VERILATOR_SIMFLAG_NPC) $(VSRC_NPC) $(SIM_CSRC_NPC) 
	$(NPC_SIM_OBJ_DIR)/V${USER_ID}_$(TOPNAME) $(NPC_FLAG)
ifneq ($(WAVE),)
	@echo "wave"
	gtkwave $(NPC_SIM_OBJ_DIR)/$(WAVE_OBJ)
endif

# 查看波形
wave:
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE_OBJ)