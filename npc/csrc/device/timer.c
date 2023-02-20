/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/
/*
#include "device/map.h"
#include "device/mmio.h"
#include <sys/time.h>
#include <time.h>
#include <cpu/cpu.h>

uint64_t get_time();

static uint32_t *timer_base = NULL;

static void timer_io_handler(uint32_t offset, int len, bool is_write) {
  assert(!is_write);
  assert(offset % 8 == 0);
  uint64_t uptime = get_time();
  if (!is_write && offset == 0) {
    timer_base[0] = (uint32_t)uptime;
    timer_base[1] = uptime >> 32;
  }

  // update current time
  if(offset != 0) {
    time_t t; 
    t = time(NULL);
    struct tm *p = localtime(&t); 
    timer_base[2] = 1900 + p->tm_year;
    timer_base[3] = 1 + p->tm_mon;
    timer_base[4] = p->tm_mday;
    timer_base[5] = p->tm_hour;
    timer_base[6] = p->tm_min;
    timer_base[7] = p->tm_sec;
  }
  
}

void init_timer() {
  timer_base = (uint32_t*)new_space(32);
  add_mmio_map("timer", CONFIG_TIMER_MMIO, timer_base, 32, (io_callback_t)timer_io_handler);
}
*/