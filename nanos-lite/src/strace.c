#include <common.h>

char *get_name_by_fd(int fd);

typedef struct node {
  char name[32];
  uint64_t ret;
  uint64_t args[4];
  int fd;
  struct node *next;
} snode;

static snode *strace_head = NULL;
static snode *strace_tail = NULL;

void insert_strace(char *name, uint64_t *args, uint64_t ret, int fd) {
  snode *node = (snode*)malloc(sizeof(snode));
  strcpy(node->name, name);
  memcpy(node->args, args, sizeof(uint64_t) * 4);
  node->ret = ret;
  node->fd = fd;
  node->next = NULL;

  if (strace_head == NULL) {
    strace_head = node;
    strace_tail = node;
  }
  else {
    strace_tail->next = node;
    strace_tail = node;
  }
}

void free_strace() {
  snode *tmp;
  while(strace_head != NULL) {
    tmp = strace_head->next;
    free(strace_head);
    strace_head = tmp;
  }
}

void print_strace() {
  Log("---strace message start---");
  snode *ptr = strace_head;
  while(ptr != NULL) {
    if (ptr->fd != -1) {
      Log("syscall: name=%s, args=%d %d %d %d, ret=%d, file=%s", ptr->name, ptr->args[0], ptr->args[1], ptr->args[2], ptr->args[3], ptr->ret, get_name_by_fd(ptr->fd));
    }
    else {
      Log("syscall: name=%s, args=%d %d %d %d, ret=%d", ptr->name, ptr->args[0], ptr->args[1], ptr->args[2], ptr->args[3], ptr->ret);
    }
    ptr = ptr->next;
  }
  Log("---strace message end---");
}
