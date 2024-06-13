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

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"
// my change
#include <memory/paddr.h>

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();
void watchpoint_display();
void free_wp(int);
void new_wp(char*);

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}


static int cmd_si(char *args) {
	char *other = NULL;
	uint64_t iter_num = 1;
	if (args != NULL) {
		// convert string to integer
		iter_num = (uint64_t)strtol(args, &other, 10);
		if (other == args) {
			printf("The parameter must be an integer!\n");
			return 0;
		} 
	}
	cpu_exec(iter_num);
	return 0;
}


static int cmd_info(char *args) {
	if (args == NULL) {
		printf("Input parameters are required!\n");
		return 0;
	}
	char arg = args[0];
	switch (arg) {
		case 'r':
			printf("Print register status:\n");
			isa_reg_display();
			break;
		case 'w':
			printf("Print watchpoint information:\n");
			watchpoint_display();
			break;
		default:
			printf("Unknown parameter: %s\n", args);
	}
	return 0;
}


static int cmd_x(char *args) {
	if (args == NULL) {
		printf("Input parameters are required!\n");
		return 0;
	}

	char *n_other = NULL;
	char n[65535];
	sscanf(args, "%s", n);
	paddr_t N = (paddr_t)strtol(n, &n_other, 10);
	if (n == n_other) {
		printf("The parameter is wrong, please enter the correct parameter!\n");
		return 0;
	}

	int len = strlen(n);
	char *e = args + len;

	bool success;
	paddr_t addr = expr(e, &success);
	if (!success) {
		printf("The expression is malformed!\n");
		return 0;
	}

	// Print the data from the corresponding address
	for (paddr_t i = 0; i < N; ++ i) {
		paddr_t tmp = addr + 4 * i;
		printf("0x%016x:\t", tmp);
		for (paddr_t j = 0; j < 4; ++ j) {
			word_t data = paddr_read(tmp + j, 1);
			printf("%02lx ", data);
		}
		printf("\n");
	}
	return 0;
}


static int cmd_p(char *args) {
	if (args == NULL) {
		printf("Missing parameters!\n");
		return 0;
	}
	bool success;
	word_t res = expr(args, &success);
	if (!success) {
		printf("The expression is malformed!\n");
	}
	else {
		printf("%s=%lu\n", args, res);
	}
	return 0;
}


static int cmd_w(char *args) {
	if (args == NULL) {
		printf("Missing parameters!\n");
		return 0;
	}
#ifdef CONFIG_WATCHPOINT
	new_wp(args);
#endif
	return 0;
}


static int cmd_d(char *args) {
	if (args == NULL) {
		printf("Missing the number of the watchpoint!\n");
		return 0;
	}
	char *other = NULL;
	int no = (int)strtol(args, &other, 10);
	if (other == args) {
		printf("Parameter error!\n");
	}
	else {
		free_wp(no);
	}
	return 0;
}


static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  // my change
  nemu_state.state = NEMU_QUIT;
  return -1;
}

static int cmd_help(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Step into", cmd_si},
  { "info", "Print register status, print monitor information", cmd_info },
  { "x", "Scan memory", cmd_x},
  { "p", "Expression evaluation", cmd_p},
  { "w", "Set up watchpoints", cmd_w},
  { "d", "Delete a watchpoints", cmd_d}
  /* TODO: Add more commands */
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i --) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
