#include <fs.h>


size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
  size_t open_offset; // 当前读写的位置
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB};

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
  [FD_STDOUT] = {"stdout", 0, 0, invalid_read, invalid_write, 0},
  [FD_STDERR] = {"stderr", 0, 0, invalid_read, invalid_write, 0},
#include "files.h"
};


void init_fs() {
  // TODO: initialize the size of /dev/fb
  int n = sizeof(file_table) / sizeof(Finfo);
  for (int i = 0; i < n; i++) {
    file_table[i].open_offset = 0;
  }
}

int fs_open(const char *pathname, int flags, int mode) {
  int n = sizeof(file_table) / sizeof(Finfo);
  for (int i = 3; i < n-1; i++) {
    if (strcmp(file_table[i].name, pathname) == 0) {
      return i;
    }
  }
  panic("Failed to open file!");
}

int fs_close(int fd) {
  // todo
  return 0;
}

size_t fs_read(int fd, void *buf, size_t len) {
  size_t size = file_table[fd].size;
  size_t open_offset = file_table[fd].open_offset;
  assert(len + open_offset < size);
  size_t offset = file_table[fd].disk_offset + open_offset;
  ramdisk_read(buf, offset, len);
  file_table[fd].open_offset = open_offset + len;
  return len;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  size_t size = file_table[fd].size;
  size_t open_offset = file_table[fd].open_offset;
  assert(len + open_offset < size);
  size_t offset = file_table[fd].disk_offset + open_offset;
  ramdisk_write(buf, offset, len);
  file_table[fd].open_offset = open_offset + len;
  return len;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  switch (whence) {
    case SEEK_SET: file_table[fd].open_offset = offset; break;
    case SEEK_CUR: file_table[fd].open_offset = file_table[fd].open_offset + offset; break;
    case SEEK_END: break; // 保持文件关闭时的位置
    default: assert(0);
  }
  return 0;
}

