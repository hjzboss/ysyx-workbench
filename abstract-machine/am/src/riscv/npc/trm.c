#include <am.h>
#include <klib-macros.h>
#define SERIAL_PORT     (0xa00003f8)

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void outb(uintptr_t addr, uint8_t  data) { *(volatile uint8_t  *)addr = data; }

void putch(char ch) {
  outb(SERIAL_PORT, ' ');
}

void halt(int code) {
  asm volatile("ebreak;");
  while (1);
}

void _trm_init() {
  int ret = main(mainargs);
  halt(ret);
}
