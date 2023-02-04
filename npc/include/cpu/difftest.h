#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "cpu/cpu.h"

#ifdef CONFIG_DIFFTEST

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);

extern CPUState cpu;

typedef struct {
  uint64_t gpr[32];
  uint64_t pc;
} NEMUCPUState;

#endif
#endif