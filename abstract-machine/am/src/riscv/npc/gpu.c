#include <am.h>
#include <klib-macros.h>
#include "npc.h"
#include <riscv/riscv.h>
#include <klib.h>

#define SYNC_ADDR (VGACTL_ADDR + 8)

void __am_gpu_init() {
  // only for test initial
  /*
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  int width = data >> 16;
  int height = data & 0x0000ffff;
  int i;
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  for (i = 0; i < width * height; i ++) fb[i] = i;
  outl(SYNC_ADDR, 1);*/
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  // 获取屏幕的宽高
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = data >> 16, 
    .height = data & 0x0000ffff,
    .vmemsz = 0
  };
}

// 向屏幕(x, y)位置画一块(w, h)的矩形，然后同步
void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  // 读取屏幕的大小
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  int width = data >> 16;
  int height = data & 0x0000ffff;
  if((ctl->x + ctl->w > width) || (ctl->y + ctl->h > height)) {
    panic("am gpu out of display range");
  }

  // 获取显存
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;

  // 写入显存
  uint32_t *pixels_tmp = (uint32_t *)ctl->pixels;
  fb += width * ctl->y + ctl->x;
  for (int j = 0; j < ctl->h; j++) {
    for (int i = 0; i < ctl->w; i++) {
      outl((uintptr_t)fb++, *pixels_tmp++);
    }
    fb += width - ctl->w;
  }

  // 同步
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}