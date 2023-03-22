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

#include <isa.h>
#include <memory/host.h>
#include <memory/vaddr.h>
#include <device/map.h>
#include <utils.h>

#ifdef CONFIG_DTRACE
typedef struct node {
  paddr_t addr;
  int len;
  word_t data;
  IOMap *map;
  bool is_read;
  struct node *next;
} dnode;

static dnode *dtrace_head = NULL;
static dnode *dtrace_tail = NULL;

void insert_dtrace(paddr_t addr, int len, word_t data, IOMap *map, bool is_read) {
  dnode *n = (dnode*)malloc(sizeof(dnode));
  n->addr = addr;
  n->len = len;
  n->data = data;
  n->map = map;
  n->is_read = is_read;
  n->next = NULL;

  if (dtrace_head == NULL) {
    dtrace_head = n;
    dtrace_tail = n;
  }
  else {
    dtrace_tail->next = n;
    dtrace_tail = n;
  }
}

void free_dtrace() {
  dnode *tmp;
  while(dtrace_head != NULL) {
    tmp = dtrace_head->next;
    free(dtrace_head);
    dtrace_head = tmp;
  }
}

void print_dtrace() {
  log_write("---dtrace message start---\n");
  dnode *ptr = dtrace_head;
  while(ptr != NULL) {
    log_write("%s: ", ptr->map->name);
    log_write("[0x%016x]", ptr->addr);
    if (ptr->is_read) log_write(" --> ");
    else log_write(" <-- ");
    log_write("0x%016lx, len=%d bytes\n", ptr->data, ptr->len);

    ptr = ptr->next;
  }
  log_write("---dtrace message end---\n");
}
#endif

#define IO_SPACE_MAX (2 * 1024 * 1024)

static uint8_t *io_space = NULL;
static uint8_t *p_space = NULL;

uint8_t* new_space(int size) {
  uint8_t *p = p_space;
  // page aligned;
  size = (size + (PAGE_SIZE - 1)) & ~PAGE_MASK;
  p_space += size;
  assert(p_space - io_space < IO_SPACE_MAX);
  return p;
}

static void check_bound(IOMap *map, paddr_t addr) {
  if (map == NULL) {
    Assert(map != NULL, "address (" FMT_PADDR ") is out of bound at pc = " FMT_WORD, addr, cpu.pc);
  } else {
    if (!(addr <= map->high && addr >= map->low)) {
      printf("io_name=%s\n", map->name);
    }
    Assert(addr <= map->high && addr >= map->low,
        "address (" FMT_PADDR ") is out of bound {%s} [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, map->name, map->low, map->high, cpu.pc);
  }
}

static void invoke_callback(io_callback_t c, paddr_t offset, int len, bool is_write) {
  if (c != NULL) { c(offset, len, is_write); }
}

void init_map() {
  io_space = malloc(IO_SPACE_MAX);
  assert(io_space);
  p_space = io_space;
}

word_t map_read(paddr_t addr, int len, IOMap *map) {
  assert(len >= 1 && len <= 8);
  check_bound(map, addr);
  paddr_t offset = addr - map->low;
  invoke_callback(map->callback, offset, len, false); // prepare data to read
  word_t ret = host_read(map->space + offset, len);
  IFDEF(CONFIG_DTRACE, insert_dtrace(addr, len, ret, map, true));
  return ret;
}

void map_write(paddr_t addr, int len, word_t data, IOMap *map) {
  assert(len >= 1 && len <= 8);
  check_bound(map, addr);
  paddr_t offset = addr - map->low;
  host_write(map->space + offset, len, data);
  invoke_callback(map->callback, offset, len, true);
  IFDEF(CONFIG_DTRACE, insert_dtrace(addr, len, data, map, false));
}
