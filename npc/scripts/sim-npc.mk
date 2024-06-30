BUILD_DIR = ${NPC_HOME}/build

BLACKBOX_DIR = ${NPC_HOME}/core/src/main/verilog

USER_ID = ysyx_22050853

TOPNAME = Soc

VSRC = ${BUILD_DIR}/${TOPNAME}.v
VSRC += $(shell find $(abspath ${BLACKBOX_DIR}) -name "*.sv")

SIM_CSRC = $(shell find $(abspath ${NPC_HOME}/csrc) -name "*.cpp")

VERILATOR = verilator
SIM_OBJ_DIR = $(BUILD_DIR)/sim/obj_dir
WAVE_OBJ = wave.vcd

# npc仿真参数
NPC_FLAG += -l $(BUILD_DIR)/npc-log.txt
NPC_FLAG += -i $(IMAGE_OBJ)
NPC_FLAG += -e ${IMAGE}.elf
NPC_FLAG += -b

# difftest动态库
DIFFSET_SO := ${NEMU_HOME}/build/riscv64-nemu-interpreter-so

# 链接参数
LFLAGS += $(shell llvm-config --libs) -lreadline -ldl -pie -lSDL2

# c参数
CFLAGS += -I${NPC_HOME}/include -O2 -I/usr/lib/llvm-14/include -fno-exceptions -D_GNU_SOURCE -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS

# difftest
ifneq ($(DIFF),)
NPC_FLAG += -d ${DIFFSET_SO}
LFLAGS += $(DIFFSET_SO)
CFLAGS += -DCONFIG_DIFFTEST=1
endif

# wave
ifneq ($(WAVE),)
CFLAGS += -DCONFIG_WAVE=1
endif

# verilator仿真参数
VERILATOR_SIMFLAG = 
# build
VERILATOR_SIMFLAG += --cc --exe --build -MMD
# C++ compiler arguments for makefile
VERILATOR_SIMFLAG += -CFLAGS "${CFLAGS}"
# open trace
VERILATOR_SIMFLAG += --trace --Mdir $(SIM_OBJ_DIR)
# top module
VERILATOR_SIMFLAG += --top-module ${USER_ID}_$(TOPNAME)

IMAGE_OBJ ?= 

VERILATOR_SIMFLAG += -LDFLAGS "$(LFLAGS)"

# 仿真
sim: 
	make -C ${NPC_HOME} verilog
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(SIM_OBJ_DIR)
	@echo "build"
	$(VERILATOR) $(VERILATOR_SIMFLAG) $(SIM_CSRC) $(VSRC)
	$(SIM_OBJ_DIR)/V${USER_ID}_$(TOPNAME) $(NPC_FLAG)
ifneq ($(WAVE),)
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE_OBJ)
endif

# 查看波形
wave:
	@echo "wave"
	gtkwave $(SIM_OBJ_DIR)/$(WAVE_OBJ)