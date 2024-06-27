#include <am.h>
#include <klib.h>
#include "npc.h"

inline uint64_t ind(uint64_t addr) { return *(volatile uint64_t *)addr; }

void __am_timer_init() {
  // 初始化硬件计时器
  (uint64_t)ind(RTC_INIT);
}

// 懒得兼容32位了。。。
void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = ind(RTC_ADDR);
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  // todo
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
