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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256, TK_EQ, L_PARENTHESIS, R_PARENTHESIS, PLUS, MINUS, TIMES, DIVIDE, INTEGER,

  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   )*/

  {" +", TK_NOTYPE},						// spaces
  {"\\+", PLUS},								// plus
  {"==", TK_EQ},								// equal
	{"[1-9][0-9]*|0", INTEGER},			// integer
	{"-", MINUS},									// minus
	{"\\*", TIMES},								// times
	{"/", DIVIDE},								// divide
	{"\\(", L_PARENTHESIS},				// left parenthesis
	{"\\)", R_PARENTHESIS},				// right parenthesis
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

//static Token tokens[32] __attribute__((used)) = {};
static Token tokens[65535] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        switch (rules[i].token_type) {
					case PLUS: case TK_EQ: case MINUS: case TIMES: case DIVIDE: 
					case L_PARENTHESIS: case R_PARENTHESIS:
						tokens[nr_token].type = rules[i].token_type;
						//printf("token_type=%d, rules_type=%d\n", tokens[nr_token].type, rules[i].token_type);
						tokens[nr_token].str[0] = '\0';
						break;		
					case INTEGER:
						// TODO: 去掉多余的0
						if (substr_len >= 32) {      
							printf("matched integer is too long at position %d\n%s\n%*.s^\n", position, e, position, "");
							return false;
						} 
						else {
							tokens[nr_token].type = rules[i].token_type;
							strncpy(tokens[nr_token].str, substr_start, substr_len);
							tokens[nr_token].str[substr_len] = '\0';
						}
						break;
          default: 
						//TODO();
        }

				nr_token += 1;
        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

bool check_parentheses(int p, int q) {
	bool flag = true;
	if (tokens[p].type != L_PARENTHESIS || tokens[q].type != R_PARENTHESIS)
		flag = false;

	int top = -1;
	int index = p;
	while(index <= q) {
		if (q == 512)
			printf("index=%d\n", index);
		int type = tokens[index].type;
		if (type == L_PARENTHESIS) {
			printf("push\n");
			++top;
		}
		else if (type == R_PARENTHESIS) {
			printf("pop\n");
			if (top == -1)
				assert(0);
			else if(--top == -1 && index < q)
				flag = false;
		}
		index += 1;
	}
	
	printf("top=%d\n", top);
	assert(top == -1);
	return flag;
}

word_t eval(int p, int q) {
	printf("p=%d, q=%d, eval\n", p, q);
	if (p > q)
		assert(0);
	else if (p == q) {
		char *tmp = NULL;
		return strtol(tokens[p].str, &tmp, 10);
	} 
	else if (check_parentheses(p, q) == true) {
		return eval(p + 1, q - 1);
	}
	else {
		int op = -1;
		int op_type = -1;
		bool flag = false;
		for (int i = p; i <= q; i ++) {
			int type = tokens[i].type;
			printf("type=%d, value=%s\n", type, tokens[i].str);
			if (type == L_PARENTHESIS)
				flag = true;
			else if (type == R_PARENTHESIS)
				flag = false;
			else if (flag || type == INTEGER)
				// Operators inside parentheses are ignored
				continue;
			else {
				if (op == -1 || type <= op_type 
						|| (op_type == PLUS && type == MINUS)
						|| (op_type == TIMES && type == DIVIDE)) {
					op = i;
					assert(type!=0);
					op_type = type;
				}
			}
		}

		assert(op != -1);
		word_t val1 = eval(p, op - 1);
		word_t val2 = eval(op + 1, q);
		
		switch (op_type) {
			case PLUS: return val1 + val2; break;
			case MINUS: return val1 - val2; break;
			case TIMES: return val1 * val2; break;
			case DIVIDE: return val1 / val2; break;
			default: printf("op=%d, type=%d\n", op, op_type); assert(0);
		}
	}
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
	word_t res = eval(0, nr_token-1);
	*success = true;
	return res;
}
