#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include <stdint.h>
#include <config.h>

#ifdef CONFIG_DIFFTEST

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

extern void (*ref_difftest_memcpy)(uint64_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);

typedef struct {
  uint64_t gpr[32];
  uint64_t pc;
  uint64_t csr[CSR_NUM];
  uint32_t inst;
} NEMUCPUState;

#endif

#endif