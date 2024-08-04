#include <am.h>
#include <nemu.h>
#include <klib.h>

void __am_timer_init() {
}

// nemu中的timer寄存器设置为两个32位的，而不是64位的，目的是兼容32位的机器
// 要先访问高32位触发timer中的回调函数来更新设备寄存器的时间
void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  //uptime->us = (((uint64_t)inl(RTC_ADDR + 4)) << 32) | inl(RTC_ADDR);
  uptime->us = inl(RTC_ADDR) | (((uint64_t)inl(RTC_ADDR + 4)) << 32);
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}