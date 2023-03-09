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
  [FD_STDIN]  = {"stdin", 0, 0, invalid_read, invalid_write},
  [FD_STDOUT] = {"stdout", 0, 0, invalid_read, invalid_write},
  [FD_STDERR] = {"stderr", 0, 0, invalid_read, invalid_write},
#include "files.h"
};


void init_fs() {
  // TODO: initialize the size of /dev/fb
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
  printf("size=%d, len=%p\n", size, len);
  assert(len < size);
  size_t offset = file_table[fd].disk_offset;
  ramdisk_read(buf, offset, len);
  return len;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  size_t size = file_table[fd].size;
  assert(len < size);
  size_t offset = file_table[fd].disk_offset;
  ramdisk_write(buf, offset, len);
  return len;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  panic("fs_lseek todo");
}

