#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#include <verilated.h>

// Include model header, generated from Verilating "jzcore.v"
#include "VSoc.h"

// If "verilator --trace" is used, include the tracing class
#include <verilated_vcd_c.h>

// dpi-c
#include "svdpi.h"

#include <stdio.h>

#include <assert.h>

#include <config.h>

#include <macro.h>

#include <stdint.h>

#include <debug.h>

typedef struct {
  int state;
  uint64_t halt_pc;
  uint32_t halt_ret;
} NPCState;

extern NPCState npc_state;
enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

typedef struct {
  IFDEF(CONFIG_DIFFTEST, uint64_t gpr[32]); // 只是为了传给difftest一个初始的空寄存器组
  uint64_t pc;
  IFDEF(CONFIG_DIFFTEST, uint64_t csr[CSR_NUM]);
  uint64_t npc;
  uint32_t inst;
  IFDEF(CONFIG_ITRACE, char logbuf[128]);
} CPUState;

extern CPUState npc_cpu;

#define FMT_WORD "0x%016lx"

#endif