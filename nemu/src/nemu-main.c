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
#include <assert.h>
#include <common.h>

word_t expr(char*, _Bool*);

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();

int main(int argc, char *argv[]) {
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif
	
	// test eval
	char *src = "/home/hjz/ysyx-workbench/nemu/tools/gen-expr/input";
	FILE *fp = fopen(src, "r");
	assert(fp != NULL);
	_Bool success;
	char buf[65535] = {};
	word_t res, result;
	while (true) {
		if(fscanf(fp, "%lu", &res) == EOF)
			break;
		fgetc(fp);
		if(fgets(buf, 65535, fp) == NULL)
			break;
		result = expr(buf, &success);
		if (res == result) {
			printf("通过!\n");
		} else {
			printf("未通过!\n");
		}
	}
	fclose(fp);

  /* Start engine. */
  engine_start();

  return is_exit_status_bad();
}
