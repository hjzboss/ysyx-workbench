/*
#ifndef __MAP_H__
#define __MAP_H__

#include "commen.h"

#define PAGE_SHIFT        12
#define PAGE_SIZE         (1ul << PAGE_SHIFT)
#define PAGE_MASK         (PAGE_SIZE - 1)

// callback function
typedef void(*io_callback_t)(uint32_t, char, bool);

#ifdef CONFIG_DIFFTEST
void difftest_skip_ref();
#endif

uint8_t* new_space(int size);

typedef struct {
  const char *name;
  // we treat ioaddr_t as uint64_t here
  uint64_t low;  // Simulated virtual address in guest
  uint64_t high; // Simulated virtual address in guest
  void *space;  // real address in host
  io_callback_t callback;
} IOMap;

static inline bool map_inside(IOMap *map, uint64_t addr) {
  return (addr >= map->low && addr <= map->high);
}

static inline int find_mapid_by_addr(IOMap *maps, int size, uint64_t addr) {
  int i;
  for (i = 0; i < size; i ++) {
    if (map_inside(maps + i, addr)) {
      // skip difftest
      IFDEF(CONFIG_DIFFTEST, difftest_skip_ref());
      return i;
    }
  }
  return -1;
}

// AXI 64 bits
uint64_t map_read(uint64_t addr, int len, IOMap *map);
void map_write(uint64_t addr, int len, uint64_t data, IOMap *map);

#endif
*/