#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#include <verilated.h>

// Include model header, generated from Verilating "jzcore.v"
#include "VJzCore.h"

// If "verilator --trace" is used, include the tracing class
#include <verilated_vcd_c.h>

// dpi-c
#include "svdpi.h"

#include <stdio.h>

#include <assert.h>

#include <config.h>

#include <macro.h>

#include <stdint.h>

void init_cpu(char *);

void delete_cpu();

void main_loop();

void cpu_exec(uint64_t);

typedef struct {
  int state;
  uint64_t halt_pc;
  uint32_t halt_ret;
} NPCState;

extern NPCState npc_state;

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

#define FMT_WORD "0x%016lx"

#endif