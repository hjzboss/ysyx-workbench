/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
#endif

#ifdef CONFIG_MTRACE
typedef struct node {
  bool read;
  paddr_t addr;
  uint64_t value;
  int len;
  struct node *next;
} mtrace_node;

static mtrace_node *mtrace_head = NULL;
static mtrace_node *mtrace_tail = NULL;

static void insert_mtrace(bool is_read, paddr_t addr, int len, uint64_t value) {
  mtrace_node *node = (mtrace_node*)malloc(sizeof(mtrace_node));
  node->read = is_read;
  node->addr = addr;
  node->len = len;
  node->value = value;
  node->next = NULL;

  if (mtrace_head == NULL) {
    mtrace_head = node;
    mtrace_tail = node;
  }
  else {
    mtrace_tail->next = node;
    mtrace_tail = node;
  }
}

void free_mtrace() {
  mtrace_node *tmp;
  while(mtrace_head != NULL) {
    tmp = mtrace_head->next;
    free(mtrace_head);
    mtrace_head = tmp;
  }
}

void print_mtrace() {
  printf("---mtrace message start---\n");
  mtrace_node *ptr = mtrace_head;
  while(ptr != NULL) {
    printf("[0x%016x]", ptr->addr);
    if (ptr->read) printf(" --> ");
    else printf(" <-- ");
    printf("0x%016lx, len=%d bytes\n", ptr->value, ptr->len);

    ptr = ptr->next;
  }
  printf("---mtrace message end---\n");
}
#endif

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
#ifdef CONFIG_MEM_RANDOM
  uint32_t *p = (uint32_t *)pmem;
  int i;
  for (i = 0; i < (int) (CONFIG_MSIZE / sizeof(p[0])); i ++) {
    p[i] = rand();
  }
#endif
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
}

word_t paddr_read(paddr_t addr, int len) {
  word_t result = 0;
  if (likely(in_pmem(addr))) {
    result = pmem_read(addr, len);
    IFDEF(CONFIG_MTRACE, insert_mtrace(true, addr, len, result));
    return result;
  }
  IFDEF(CONFIG_DEVICE, result = mmio_read(addr, len);
   IFDEF(CONFIG_MTRACE, insert_mtrace(true, addr, len, result)); return result);
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  if (likely(in_pmem(addr))) { 
    pmem_write(addr, len, data); 
    IFDEF(CONFIG_MTRACE, insert_mtrace(false, addr, len, data));
    return; }
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); IFDEF(CONFIG_MTRACE, insert_mtrace(false, addr, len, data)); return);
  out_of_bound(addr);
}
