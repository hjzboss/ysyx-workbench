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

#include "config.h"

void init_cpu(char *dir);

void delete_cpu();

void one_cycle();

void main_loop();

extern int npc_state;

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };
#endif