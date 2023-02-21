/*
#ifndef __MMIO_H__
#define __MMIO_H__

#include <stdint.h>
#include <device/map.h>

uint64_t mmio_read(uint64_t addr);
void mmio_write(uint64_t addr, word_t data, uint8_t mask);
void add_mmio_map(const char *name, uint64_t addr, void *space, uint32_t len, io_callback_t callback);

#endif
*/