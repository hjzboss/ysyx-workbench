#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__
#include <stdint.h>
#include <config.h>

#define PMEM_LEFT  ((uint64_t)CONFIG_MBASE)
#define PMEM_RIGHT ((uint64_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

static inline bool in_pmem(uint64_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(uint64_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
uint64_t host_to_guest(uint8_t *haddr);

uint64_t paddr_read(uint64_t addr, int len);
void paddr_write(uint64_t addr, int len, uint64_t data);

#endif