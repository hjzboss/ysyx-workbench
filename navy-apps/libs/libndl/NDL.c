#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <assert.h>

int _open(const char *path, int flags, mode_t mode);

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0, screen_x = 0, screen_y = 0; // canvas size and location
static int fb_w = 0, fb_h = 0; // frame buffer size
static int fb_fd;

uint32_t NDL_GetTicks() {
  struct timeval *tv = (struct timeval *)malloc(sizeof(struct timeval));
  gettimeofday(tv, NULL);
  return tv->tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  // keyboard input
  int fd = _open("/dev/events", 0, 0);
  return read(fd, buf, len) == 0 ? 0 : 1;
}

// 打开一张(*w) X (*h)的画布
// 如果*w和*h均为0, 则将系统全屏幕作为画布, 并将*w和*h分别设为系统屏幕的大小
void NDL_OpenCanvas(int *w, int *h) {
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
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
  int fd = _open("/proc/disinfo", 0, 0);
  size_t len = read(fd, buf, 32);
  // get frame buffer size
  char *first_item = strtok(buf, "\n");
  char *second_item = strtok(NULL, "\n");
  strtok(first_item, " ");
  char *width_s = strtok(NULL, " ");
  assert(width_s != NULL);
  strtok(second_item, " ");
  char *height_s = strtok(NULL, " ");
  assert(height_s != NULL);

  fb_w = atoi(width_s);
  fb_h = atoi(height_s);
  assert(fb_w > 0 && fb_h > 0);
  
  // 判断画布的大小是否正常
  int canvas_w = *w;
  int canvas_h = *h;
  if (canvas_w > fb_w || canvas_w == 0)
    screen_w = fb_w;
  else
    screen_w = canvas_w;

  if (canvas_h > fb_h || canvas_h == 0)
    screen_h = fb_h;
  else
    screen_h = canvas_h;

  *w = screen_w;
  *h = screen_h;

  screen_x = fb_w / 2 - screen_w / 2;
  screen_y = fb_h / 2 - screen_h / 2;
}

// 向画布`(x, y)`坐标处绘制`w*h`的矩形图像, 并将该绘制区域同步到屏幕上
// 图像像素按行优先方式存储在`pixels`中, 每个像素用32位整数以`00RRGGBB`的方式描述颜色
void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  int fb_fd = _open("/dev/fb", 0, 0);

  assert(x + screen_x >= 0 && x + screen_x + w <= fb_w);
  assert(y + screen_y >= 0 && y + screen_y + h <= fb_h);

  for(int i = 0; i < h; i++) {
    // 逐行绘制
    lseek(fb_fd, ((y + screen_y + i) * fb_w + x + screen_x) * 4, SEEK_SET);
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
