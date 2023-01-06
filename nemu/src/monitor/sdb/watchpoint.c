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

#include "sdb.h"

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;

  /* TODO: Add more members if necessary */
	char *expr;
	word_t value;
} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
		wp_pool[i].expr = NULL;
		wp_pool[i].value = 0;
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

void new_wp(char *e) {
	if (free_ == NULL) {
		printf("There are no idle watchpoints to assign!\n");
		return;
	}
	bool success;
	word_t res = expr(e, &success);
	if (success) {
		WP *w = free_;
		free_ = free_->next;
		w->next = head;
		w->expr = e;
		w->value = res;
		head = w;
		printf("Hardware watchpoint %d: %s\n", head->NO, head->expr);
	}
}

void insert_free(WP *wp) {
	if (free_ == NULL) {
		free_ = wp;
		wp->next = NULL;
	}
	else if (free_->next == NULL) {
		if (wp->NO > free_->NO) {
			free_->next = wp;
			wp->next = NULL;
		}
		else {
			wp->next = free_;
			free_ = wp;
		}
	}
	else {
		WP *p = free_;
		while (p->next && p->next->NO < wp->NO) { p = p->next; }
		wp->next = p->next;
		p->next = wp;	
	}
}

void free_wp(int no) {
	if (head == NULL || (head->next == NULL && head->NO != no)) {
		printf("No target watchpoint found!\n");
	}
	else if (head->NO == no) {
		WP *tmp = head;
		head = head->next;
		insert_free(tmp);
		printf("Delete the watchpoint %d: %s\n", tmp->NO, tmp->expr);
	}
	else {
		WP *pre = head;
		WP *cur = head->next;
		while (cur) {
			if (cur->NO == no) {
				pre->next = cur->next;
				insert_free(cur);
				printf("Delete the watchpoint %d: %s\n", cur->NO, cur->expr);
				return;
			}
			else {
				cur = cur->next;
				pre = pre->next;
			}
		}
		printf("No target watchpoint found!\n");
	}
}

// Scans all non-idle watchpoints and returns true if the result of the expression changes
void scan_watchpoint() {
	WP *cur = head;
	bool flag = false;
	while (cur) {
		bool success;
		printf("debug: %d, %s, %lu\n", cur->NO, cur->expr, cur->value);
		word_t n_val = expr(cur->expr, &success);
		assert(success);
		if (n_val != cur->value) {
			printf("Hardware watchpoint %d: %s\n\n", cur->NO, cur->expr);
			printf("Old value = %lu\n", cur->value);
			printf("New value = %lu\n\n", n_val);
			flag = true;
			cur->value = n_val;
		}
	}
	
	if (flag) {
		nemu_state.state = NEMU_STOP;
	}
}

void watchpoint_display() {
	printf("NO\t\tWhat\t\tValue\n");
	WP *cur = head;
	while (cur) {
		printf("%d\t\t%s\t\t%lu\n", cur->NO, cur->expr, cur->value);
		cur = cur->next;
	}
	printf("\n");
}
