#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

void __am_gpu_init() {
  // only for test initial
  /*
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  int width = data >> 16;
  int height = data & 0x0000ffff;
  int i;
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  for (i = 0; i < width * height; i ++) fb[i] = i;
  outl(SYNC_ADDR, 1);
  */
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = data >> 16, 
    .height = data & 0x000fffff,
    .vmemsz = 0
  };
}

// 向屏幕(x, y)坐标处绘制w*h的矩形图像， 图像像素按行优先方式存储在pixels中, 每个像素用32位整数以00RRGGBB的方式描述颜色
void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  // 获取屏幕宽高
  uint32_t data = (uint32_t)inl(VGACTL_ADDR);
  int width = data >> 16;
  int height = data & 0x0ffff;
  if((ctl->x + ctl->w > width) || (ctl->y + ctl->h > height)) {
    panic("out of display range");
  }

  // 获取显存
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;

  // 写入显存
  uint32_t *pixels_tmp = (uint32_t *)ctl->pixels;
  fb += width * ctl->y + ctl->x; // 定位屏幕
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
