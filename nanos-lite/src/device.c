#include <common.h>

void putch(char ch);

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  char *buf_t = (char *)buf;
  for (int i = 0; i < len; i++) {
    putch(*buf_t++);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  AM_INPUT_KEYBRD_T ev = io_read(AM_INPUT_KEYBRD);
  if (ev.keycode == AM_KEY_NONE) {
    return 0;
  }
  sprintf(buf, "%s %d %d", keyname[ev.keycode], ev.keycode, ev.keydown ? 1 : 0);
  return strlen(buf);
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T info = io_read(AM_GPU_CONFIG);
  int w = info.width, h = info.height;
  sprintf(buf, "WIDTH: %d\nHEIGHT: %d\n", w, h);
  return strlen(buf);
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T info = io_read(AM_GPU_CONFIG);
  int w = info.width, h = info.height;
  // 计算坐标
  int xy = offset / 4;
  int x = xy % w;
  int y = xy / w;
  assert(y <= h);
  // 向(x, y)处写入len个字节，由于是按像素大小(32位)写入，因此写入的长度是len / 4
  io_write(AM_GPU_FBDRAW, x, y, (char *)buf, len / 4, 1, true);
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
