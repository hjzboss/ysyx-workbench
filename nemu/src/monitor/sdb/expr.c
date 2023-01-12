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
// my change
#include<memory/paddr.h>

enum {
  TK_NOTYPE = 256, UNDET, AND, TK_EQ, NOT_EQ, L_PARENTHESIS, R_PARENTHESIS, PLUS, MINUS, TIMES, DIVIDE, INTEGER, POINT, REG, HEX,

  /* TODO: Add more token types */

};

static char *integer_max = "18446744073709551615";
static char *hex_max = "0xffffffffffffffff";

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   )*/

  {" +", TK_NOTYPE},																						// spaces
  {"\\+", PLUS},																								// plus
  {"==", TK_EQ},																								// equal
	{"0x[0-9a-f]+", HEX},																					// hex
	{"\\$(0|ra|gp|t[p0-6]|s[p0-11]|a[0-7])", REG},								// reg
	{"[0-9]+", INTEGER},																					// integer
	{"-", MINUS},																									// minus
	{"\\*", UNDET},																								// times and pointer dereference
	{"/", DIVIDE},																								// divide
	{"\\(", L_PARENTHESIS},																				// left parenthesis
	{"\\)", R_PARENTHESIS},																				// right parenthesis
	{"!=", NOT_EQ},																								// not equal
	{"&&", AND},																									// and
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
					case PLUS: case TK_EQ: case MINUS: case DIVIDE: 
					case L_PARENTHESIS: case R_PARENTHESIS:
						tokens[nr_token].type = rules[i].token_type;
						tokens[nr_token].str[0] = '\0';
						nr_token ++;
						break;
					case REG:
						tokens[nr_token].type = rules[i].token_type;
						strncpy(tokens[nr_token].str, substr_start, substr_len);
						tokens[nr_token].str[substr_len] = '\0';
						nr_token ++;
						break;
					case UNDET:
						// Distinguish between pointer dereference symbols and multiplication symbols
						if (nr_token == 0) {
							tokens[nr_token].type = POINT;
						}
						else {
							int nr_tmp = nr_token - 1;
							if (tokens[nr_tmp].type == INTEGER || tokens[nr_tmp].type == R_PARENTHESIS
									|| tokens[nr_tmp].type == HEX || tokens[nr_tmp].type == REG)
								tokens[nr_token].type = TIMES;
							else
								tokens[nr_token].type = POINT;
						}
						tokens[nr_token].str[0] = '\0';
						nr_token ++;
						break;
					case HEX:
					case INTEGER:
						int type = rules[i].token_type;
						if (substr_len >= 65535) {      
							printf("matched integer is too long at position %d\n%s\n%*.s^\n", position, e, position, "");
							return false;
						} 
						else {
							int index = type == INTEGER ? 0 : 2;
							int len = type == INTEGER ? 0 : 2;
							// Eliminate the extra 0s at the beginning of the string
							while (index <= substr_len - 1) {
								if (*(substr_start + index) == '0') {
									if (index == substr_len - 1) {
										len += 1;
										break;
									}
									++ index;
								}
								else {
									if (len + substr_len - index >= 32) {
										printf("matched integer is too long at position %d\n%s\n%*.s^\n", position, e, position, "");
										return false;
									}
									len = substr_len - index;
									break;
								}
							}
							// Copy positive integers into tokens array
							char *s = substr_start + index;
							tokens[nr_token].type = rules[i].token_type;
							if (type == HEX) {
								tokens[nr_token].str[0] = '0';
								tokens[nr_token].str[1] = 'x';
								strncpy(tokens[nr_token].str + 2, s, len);
								tokens[nr_token].str[len + 2] = '\0';
							}
							else {
								strncpy(tokens[nr_token].str, s, len);
								tokens[nr_token].str[len] = '\0';
							}
							nr_token ++;
						}
						break;
          default: 
        }
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
		int type = tokens[index].type;
		if (type == L_PARENTHESIS) {
			++top;
		}
		else if (type == R_PARENTHESIS) {
			if (top == -1)
				assert(0);
			else if(--top == -1 && index < q)
				flag = false;
		}
		index += 1;
	}
	
	assert(top == -1);
	return flag;
}

static word_t is_overflow(word_t val1, word_t val2, int op) {
	word_t result;
	switch(op) {
		case PLUS:
			result = val1 + val2;
			if (result < val1 || result < val2) {
				printf("An overflow occurs during addition: %lu+%lu\n", val1, val2);
				assert(0);
			}
			break;
		case MINUS:
			result = val1 - val2;
			if (result > val1) {
				printf("An overflow occurs during subtraction: %lu-%lu\n", val1, val2);
				assert(0);
			}
			break;
		case TIMES:
			result = val1 * val2;
			if (val1 && result / val1 != val2) {
				printf("An overflow occurs during multiplication: %lu*%lu\n", val1, val2);
				assert(0);
			}
			break;
		case DIVIDE:
			if (!val2) {
				printf("divide zero! error evaluation: %lu / %lu\n", val1, val2);
				assert(0);
			}
			result = val1 / val2;
			break;
		case POINT:
			if (val2 > 4294967295) {
				printf("An overflow occurs during pointer dereference: *%lu\n", val2);
				assert(0);				
			}
			result = paddr_read(val2, 4);
			break;
		default:
			printf("Unknown operator!\n");
			assert(0);
	}

	return result;
}

word_t eval(int p, int q) {
	//printf("p=%d, q=%d\n", p, q);
	if (p > q)
		assert(0);
	else if (p == q) {
		if (tokens[p].type == REG) {	
			bool reg_success;
			word_t reg = isa_reg_str2val(tokens[p].str + 1, &reg_success);
			assert(reg_success);
			return reg;
		}
		else if (tokens[p].type == INTEGER) {
			char *tmp = NULL;
			// Check for integer overflow
			if (strcmp(tokens[p].str, integer_max)) {
				printf("Integer overflow: %s\n", tokens[p].str);
				assert(0);
			};
			return strtol(tokens[p].str, &tmp, 10);
		}
		else if (tokens[p].type == HEX) {
			char *tmp = NULL;
			if (strcmp(tokens[p].str, hex_max) > 0) {
				printf("Integer overflow: %s\n", tokens[p].str);
				assert(0);					
			}
			return strtol(tokens[p].str, &tmp, 16);
		}
		else {
			printf("Error type!\n");
			assert(0);
		}
	} 
	else if (check_parentheses(p, q) == true) {
		return eval(p + 1, q - 1);
	}
	else {
		int op = -1;
		int op_type = -1;
		int cnt = 0;
		for (int i = p; i <= q; i ++) {
			int type = tokens[i].type;
			if (type == L_PARENTHESIS)
				cnt ++;
			else if (type == R_PARENTHESIS)
				cnt --;
			else if (cnt || type == INTEGER || type == REG || type == HEX)
				// Operators inside parentheses are ignored
				continue;
			else {
				if (op == -1 || type <= op_type
						|| (op_type == TK_EQ && type == NOT_EQ)
						|| (op_type == PLUS && type == MINUS)
						|| (op_type == TIMES && type == DIVIDE)) {
					op = i;
					assert(type != 0);
					op_type = type;
				}
			}
		}
		
		assert(op != -1);
		word_t val1, val2;

		if (op_type != POINT)
			val1 = eval(p, op - 1);
		else
			val1 = 0;
		val2 = eval(op + 1, q);

		switch (op_type) {
			case PLUS: return is_overflow(val1, val2, PLUS); break;
			case MINUS: return is_overflow(val1, val2, MINUS); break;
			case TIMES: return is_overflow(val1, val2, TIMES); break;
			case DIVIDE: return is_overflow(val1, val2, DIVIDE); break;
			case AND: return val1 && val2; break;
			case TK_EQ: return val1 == val2; break;
			case NOT_EQ: return val1 != val2; break;
			case POINT: return is_overflow(val1, val2, POINT); break;
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
