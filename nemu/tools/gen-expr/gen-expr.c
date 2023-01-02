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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// this should be enough
static char buf[65536] = {};
static char code_buf[65536 + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

static int sub = 0; // The current buf subscript

uint32_t choose(uint32_t n) {
	return rand() % n;
}

void insertNull() {
	if (choose(10) > 4)
		buf[sub++] = ' ';
}

static void gen_rand_expr() {
  //buf[0] = '\0';
	switch (choose(3)) {
		case 0:
			//insertNull();
			int a = rand() % 100;
			char num[32];
			sprintf(num, "%d", a);
			strcpy(buf + sub, num);
			sub += strlen(num);
			//insertNull();
			break;
		case 1:
			buf[sub] = '(';
			++sub;
			gen_rand_expr();
			buf[sub] = ')';
			++sub;
			buf[sub] = '\0';
			break;
		default:
			gen_rand_expr();
			uint32_t type = choose(4);
			switch (type) {
				case 0: buf[sub] = '+'; break;
				case 1: buf[sub] = '-'; break;
				case 2: buf[sub] = '*'; break;
				case 3: buf[sub] = '/'; break;
				default: buf[sub] = '+';
			}
			++sub;
			gen_rand_expr();
			break;
	}
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
		// my change
    sscanf(argv[1], "%d", &loop);
		//assert(tmp == 1);
  }
  int i;
  for (i = 0; i < loop; i ++) {
		sub = 0; // my change

    gen_rand_expr();

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;

    int tmp = fscanf(fp, "%d", &result);
    //assert(tmp == 1);
		if (tmp == 1) {}

		pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
