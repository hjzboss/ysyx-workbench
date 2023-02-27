#ifndef __CONFIG__
#define __CONFIG__

//#define CONFIG_WAVE 1 // 启动波形仿真

#define CSR_NUM 4

#define MAX_SIM_TIME 1000 // max simulation time
#define CONFIG_TRACE 1
#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_ITRACE 1
#define CONFIG_FTRACE 1
//#define CONFIG_DIFFTEST 1
#define CONFIG_MTRACE 1

#define CONFIG_DEVICE 1
#define CONFIG_TIMER_MMIO 0xa0000048
#define CONFIG_SERIAL_MMIO 0xa00003f8

#endif