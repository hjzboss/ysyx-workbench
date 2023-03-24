#include <cpu/cpu.h>
#include <memory/paddr.h>

static uint8_t i_cache[10000000000000000] = {};

uint8_t* guest_to_host(uint64_t paddr) { return i_cache + paddr - CONFIG_MBASE; }
uint64_t host_to_guest(uint8_t *haddr) { return haddr - i_cache + CONFIG_MBASE; }


static void out_of_bound(uint64_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, npc_cpu.pc);
}

#ifdef CONFIG_MTRACE
typedef struct node {
  bool read;
  uint64_t addr;
  uint64_t value;
  int len;
  struct node *next;
} mtrace_node;

static mtrace_node *mtrace_head = NULL;
static mtrace_node *mtrace_tail = NULL;

static void insert_mtrace(bool is_read, uint64_t addr, int len, uint64_t value) {
  mtrace_node *node = (mtrace_node*)malloc(sizeof(mtrace_node));
  node->read = is_read;
  node->addr = addr;
  node->len = len;
  node->value = value;
  node->next = NULL;
  
  // log
  log_write("[0x%016lx]", node->addr);
  if (node->read) log_write(" --> ");
  else log_write(" <-- ");
  log_write("0x%016lx, len=%d bytes\n", node->value, node->len);

  if (mtrace_head == NULL) {
    mtrace_head = node;
    mtrace_tail = node;
  }
  else {
    mtrace_tail->next = node;
    mtrace_tail = node;
  }
}

void free_mtrace() {
  mtrace_node *tmp;
  while(mtrace_head != NULL) {
    tmp = mtrace_head->next;
    free(mtrace_head);
    mtrace_head = tmp;
  }
}

void print_mtrace() {
  printf("---mtrace message start---\n");
  mtrace_node *ptr = mtrace_head;
  while(ptr != NULL) {
    printf("[0x%016lx]", ptr->addr);
    if (ptr->read) printf(" --> ");
    else printf(" <-- ");
    printf("0x%016lx, len=%d bytes\n", ptr->value, ptr->len);

    ptr = ptr->next;
  }
  printf("---mtrace message end---\n");
}
#endif

static inline uint64_t host_read(void *addr, int len) {
  switch (len) {
    case 1: return *(uint8_t  *)addr;
    case 2: return *(uint16_t *)addr;
    case 4: return *(uint32_t *)addr;
    case 8: return *(uint64_t *)addr;
    default: return 0;
  }
}

static inline void host_write(void *addr, int len, uint64_t data) {
  switch (len) {
    case 1: *(uint8_t  *)addr = data; return;
    case 2: *(uint16_t *)addr = data; return;
    case 4: *(uint32_t *)addr = data; return;
    case 8: *(uint64_t *)addr = data; return;
    default: assert(0);
  }
}

uint64_t paddr_read(uint64_t addr, int len) {
  if (in_pmem(addr)) {
    uint64_t ret = host_read(guest_to_host(addr), len);
    IFDEF(CONFIG_MTRACE, insert_mtrace(true, addr, len, ret));
    return ret;
  }
  out_of_bound(addr);
}


void paddr_write(uint64_t addr, int len, uint64_t data) {
  if (in_pmem(addr)) {
    IFDEF(CONFIG_MTRACE, insert_mtrace(false, addr, len, data));
    host_write(guest_to_host(addr), len, data);
    return;
  }
  out_of_bound(addr);
}


long load_img(char *dir) {
  FILE *fp = fopen(dir, "rb");

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(i_cache, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}