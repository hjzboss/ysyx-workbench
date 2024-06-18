#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <assert.h>


static int evtdev = -1;
static int fbdev = -1;
static int canvas_w = 0, canvas_h = 0, canvas_x = 0, canvas_y = 0; // 打开的画布的宽高
static int screen_w = 0, screen_h = 0; // frame buffer size，屏幕的宽高
static int fb_fd;

// 以毫秒为单位返回系统时间
uint32_t NDL_GetTicks() {
  struct timeval *tv = (struct timeval *)malloc(sizeof(struct timeval));
  gettimeofday(tv, NULL);
  uint32_t res = tv->tv_usec / 1000;
  free(tv);
  return res;
}

int NDL_PollEvent(char *buf, int len) {
  // keyboard input
  int fd = open("/dev/events", 0, 0);
  return read(fd, buf, len) == 0 ? 0 : 1;
}

// 打开一张(*w) X (*h)的画布
// 如果*w和*h均为0, 则将系统全屏幕作为画布, 并将*w和*h分别设为系统屏幕的大小
void NDL_OpenCanvas(int *w, int *h) {
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    canvas_w = *w; canvas_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", canvas_w, canvas_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  }
  char buf[50];
  int fd = open("/proc/disinfo", 0, 0);
  size_t len = read(fd, buf, 32);
  assert(len != -1);
  
  // get frame buffer size
  char *width_s = strtok(buf, " ");
  char *height_s = strtok(NULL, " ");
  assert(width_s != NULL);
  assert(height_s != NULL);

  screen_w = atoi(width_s);
  screen_h = atoi(height_s);
  assert(screen_w > 0 && screen_h > 0);
  
  // 判断画布的大小是否正常，如果不正常就将画布的大小设置为原始屏幕的大小
  int canvas_w = *w;
  int canvas_h = *h;
  if (canvas_w > screen_w || canvas_w == 0)
    canvas_w = screen_w;
  else
    canvas_w = canvas_w;

  if (canvas_h > screen_h || canvas_h == 0)
    canvas_h = screen_h;
  else
    canvas_h = canvas_h;

  *w = canvas_w;
  *h = canvas_h;

  // 将画布的坐标轴定位在屏幕中间
  canvas_x = screen_w / 2 - canvas_w / 2;
  canvas_y = screen_h / 2 - canvas_h / 2;
}

// 向画布`(x, y)`坐标处绘制`w*h`的矩形图像, 并将该绘制区域同步到屏幕上
// 图像像素按行优先方式存储在`pixels`中, 每个像素用32位整数以`00RRGGBB`的方式描述颜色
// 来自于miniSdl的调用，sdl传入的是颜色数据
void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  int fb_fd = open("/dev/fb", 0, 0);
  assert(x + canvas_x >= 0 && x + canvas_x + w <= screen_w);
  assert(y + canvas_y >= 0 && y + canvas_y + h <= screen_h);

  for(int i = 0; i < h; i++) {
    // 逐行绘制
    lseek(fb_fd, ((y + canvas_y + i) * screen_w + x + canvas_x) * 4, SEEK_SET);
    // 一次绘制一行
    write(fb_fd, pixels + i * w, w * 4);
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  return 0;
}

void NDL_Quit() {
  close(fb_fd);
}
