#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(uint64_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
uint64_t host_to_guest(uint8_t *haddr);

uint64_t pmem_read(uint64_t addr, int len);
//void paddr_write(paddr_t addr, int len, word_t data);

#endif