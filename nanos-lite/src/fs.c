#include <fs.h>

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);
size_t serial_write(const void *buf, size_t offset, size_t len);
size_t events_read(void *buf, size_t offset, size_t len);
size_t fb_write(const void *buf, size_t offset, size_t len);
size_t dispinfo_read(void *buf, size_t offset, size_t len);

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset; // 文件在磁盘中的偏移量
  ReadFn read;
  WriteFn write;
  size_t open_offset; // 当前读写的位置偏移量
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB, FD_DISINFO, FD_EVENTS};

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
  [FD_STDIN]  = {"stdin", 0, 0, invalid_read, invalid_write, 0},
  [FD_STDOUT] = {"stdout", 0, 0, invalid_read, serial_write, 0},
  [FD_STDERR] = {"stderr", 0, 0, invalid_read, serial_write, 0},
  [FD_FB]     = {"/dev/fb", 0, 0, invalid_read, fb_write, 0}, // 显存
  [FD_DISINFO]= {"/proc/disinfo", 0, 0, dispinfo_read, invalid_write, 0}, // 获取屏幕大小
  [FD_EVENTS] = {"/dev/events", 0, 0, events_read, invalid_write, 0},
#include "files.h"
};

static size_t file_num = 0;

char *get_name_by_fd(int fd) {
  for (int i = 0; i < file_num; i++) {
    if (i == fd) {
      return file_table[i].name;
    }
  }
  assert(0);
}

// 初始化文件系统
void init_fs() {
  // TODO: initialize the size of /dev/fb
  file_num = sizeof(file_table) / sizeof(Finfo);
  for (int i = 0; i < file_num; i++) {
    file_table[i].open_offset = 0;
    if (i == FD_FB) {
      AM_GPU_CONFIG_T cfg = io_read(AM_GPU_CONFIG);
      // 初始化显存的大小，每个像素32位(4字节)
      // 每个像素是`00rrggbb`的形式, 8位颜色
      file_table[i].size = cfg.width * cfg.height * 4;
      Log("nanos initial gpu size: %lu, width=%d, height=%d", file_table[i].size, cfg.width, cfg.height);
    }
  }
}

// 返回文件的编号
int fs_open(const char *pathname, int flags, int mode) {
  for (int i = FD_FB; i < file_num; i++) {
    if (strcmp(file_table[i].name, pathname) == 0) {
      return i;
    }
  }
  panic("Failed to open file: %s", pathname);
}

int fs_close(int fd) {
  // todo
  file_table[fd].open_offset = 0;
  return 0;
}

size_t fs_read(int fd, void *buf, size_t len) {
  size_t size = file_table[fd].size;
  size_t open_offset = file_table[fd].open_offset;
  size_t offset = file_table[fd].disk_offset + open_offset;
  size_t upper_bound = file_table[fd].disk_offset + size;
  size_t rem = offset + len > upper_bound ? (upper_bound - offset) : len;
  if (file_table[fd].read != NULL) {
    return file_table[fd].read(buf, offset, rem);
  }
  else {
    ramdisk_read(buf, offset, rem);
  }
  file_table[fd].open_offset = open_offset + rem;
  return rem;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  if (fd == FD_STDOUT || fd == FD_STDERR) {
    file_table[fd].write(buf, 0, len);
    return len;
  }
  size_t size = file_table[fd].size;
  size_t open_offset = file_table[fd].open_offset;
  assert(len + open_offset <= size);
  size_t offset = file_table[fd].disk_offset + open_offset;
  size_t upper_bound = file_table[fd].disk_offset + size;
  size_t rem = offset + len > upper_bound ? (upper_bound - offset) : len;
  if (file_table[fd].write != NULL) {
    file_table[fd].write(buf, offset, rem);
  }
  else {
    ramdisk_write(buf, offset, rem);
  }
  file_table[fd].open_offset = open_offset + rem;
  return rem;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  //printf("fuck it: fd=%d, offset=%d\n", fd, offset);
  switch (whence) {
    case SEEK_SET: file_table[fd].open_offset = offset; /*printf("set offset=%d\n", offset);*/ return offset;
    case SEEK_CUR: file_table[fd].open_offset = file_table[fd].open_offset + offset; /*printf("cur offset=%d\n", file_table[fd].open_offset);*/ return file_table[fd].open_offset;
    case SEEK_END: file_table[fd].open_offset = file_table[fd].size + offset; /*printf("end offset=%d\n", file_table[fd].open_offset);*/ return file_table[fd].open_offset;// 保持文件关闭时的位置
    default: assert(0);
  }
  return -1;
}

