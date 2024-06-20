#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

// c是trap.s中的sp，a0传了个指针
Context* __am_irq_handle(Context *c) {
  //printf("mcause=%x, mstatus=%x, mepc=%p\n", c->mcause, c->mstatus, c->mepc);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      // 系统调用
      case 0xb:
        if (c->GPR1 == -1) ev.event = EVENT_YIELD;
        else ev.event = EVENT_SYSCALL;
        break;
      default: 
        ev.event = EVENT_ERROR; 
        printf("EVENT_ERROR!");
        assert(0);
        break;
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

// nanos在初始化的时候调用，用于注册异常处理函数
bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler; // 操作系统的异常处理函数

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
