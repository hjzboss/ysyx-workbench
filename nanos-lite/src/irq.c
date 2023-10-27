#include <common.h>

void do_syscall(Context *c);

static Context* do_event(Event e, Context* c) {
  printf("fuck event\n");
  // riscv使用软件来将epc+4，因为epc保存的是异常指令本身的地址，需要软件来判断是否+4(缺页错误就不需要+4)
  switch (e.event) {
    case EVENT_YIELD: 
      c->mepc += 4;
      Log("event_type: yield"); break;
    case EVENT_SYSCALL:
      c->mepc += 4;
      do_syscall(c);
      break;
    default: panic("Unhandled event ID = %d", e.event);
  }
  return c;
}

void init_irq(void) {
  Log("Initializing interrupt/exception handler...");
  cte_init(do_event);
}
