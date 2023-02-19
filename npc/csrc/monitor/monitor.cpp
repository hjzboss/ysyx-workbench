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

#include <stdio.h>
#include <stdlib.h>
#include <cpu/cpu.h>

//void init_rand();
void init_log(const char *log_file);
//void init_mem();
IFDEF(CONFIG_DIFFTEST, void init_difftest(char *ref_so_file, long img_size));
//void init_device();
void init_sdb();
void init_disasm(const char *triple);
void init_elf(const char *file);
long init_cpu(char *);
IFDEF(CONFIG_ITRACE, void init_iringbuf());
long init_cpu(char *);



static void welcome() {
  Log("Trace: %s", MUXDEF(CONFIG_TRACE, ANSI_FMT("ON", ANSI_FG_GREEN), ANSI_FMT("OFF", ANSI_FG_RED)));
  IFDEF(CONFIG_TRACE, Log("If trace is enabled, a log file will be generated "
        "to record the trace. This may lead to a large log file. "
        "If it is not necessary, you can disable it in menuconfig"));
  //Log("Build time: %s, %s", __TIME__, __DATE__);
  printf("Welcome to %s-npc!\n", ANSI_FMT(str(riscv64), ANSI_FG_YELLOW ANSI_BG_RED));
  printf("For help, type \"help\"\n");
}

#include <getopt.h>

void sdb_set_batch_mode();

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static int difftest_port = 1234;
static char *elf_file = NULL;

static int parse_args(int argc, char *argv[]) {
  const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"img"      , required_argument, NULL, 'i'},
    {"elf"      , required_argument, NULL, 'e'},
    {0          , 0                , NULL,  0 },
  };
  int o; 
  while ( (o = getopt_long(argc, argv, "-bhl:d:p:i:e:", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'i': img_file = optarg; break;
      case 'e': elf_file = optarg; break;
      //case 1: img_file = optarg; return 0;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\t-i,--img=FILE           img file\n");
        printf("\t-e,--elf=FILE           elf file\n");
        printf("\n");
        exit(0);
    }
  }
  return 0;
}

void init_monitor(int argc, char *argv[]) {
  /* Perform some global initialization. */

  /* Parse arguments. */
  parse_args(argc, argv);

  /* Initialize devices. */
  //IFDEF(CONFIG_DEVICE, init_device());

  init_elf(elf_file);

  init_log(log_file);

  /* Perform ISA dependent initialization. */
  //init_isa();
  
  long size = init_cpu(img_file);
  printf("debug\n\n\n\n\n\n\n\n");

  /* Load the image to memory. This will overwrite the built-in image. */
  //long img_size = load_img();

  /* Initialize differential testing. */
  IFDEF(CONFIG_DIFFTEST, init_difftest(diff_so_file, size));

  /* Initialize the simple debugger. */
  init_sdb();


  IFDEF(CONFIG_ITRACE, init_disasm("riscv64" "-pc-linux-gnu"));

  IFDEF(CONFIG_ITRACE, init_iringbuf());

  /* Display welcome message. */
  welcome();
}
