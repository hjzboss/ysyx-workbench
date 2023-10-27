#include <am.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  printf("mcause=%x, mstatus=%x, mepc=%p\n", c->mcause, c->mstatus, c->mepc);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      // 系统调用
      case 0xb:
        if (c->GPR1 == -1) ev.event = EVENT_YIELD;
        else ev.event = EVENT_SYSCALL;
        // riscv使用软件来将epc+4，因为epc保存的是异常指令本身的地址，需要软件来判断是否+4(缺页错误就不需要+4)
        c->mepc += 4;
        break;
      default: ev.event = EVENT_ERROR; break;
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  return NULL;
}

void yield() {
  asm volatile("li a7, -1; ecall");
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
