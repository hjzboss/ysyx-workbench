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

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

WP* new_wp() {
	if (free_ == NULL) {
		printf("There are no idle watchpoints to assign!\n");
		assert(0);
	}
	WP *w = free_;
	free_ = free_->next;
	w->next = head;
	head = w;
	return w;
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

void free_wp(WP *wp) {
	if (head == NULL || (head->next == NULL && head != wp)) {
		printf("There are no watchpoints that can be released!\n");
		assert(0);
	}
	else if (head == wp) {
		insert_free(wp);
		head = head->next;
	}
	else {
		WP *pre = head;
		WP *cur = head->next;
		bool flag = false;
		while (cur) {
			if (cur == wp) {
				insert_free(wp);
				pre->next = cur->next;
				flag = true;
			}
			cur = cur->next;
			pre = pre->next;
		}
		if (!flag) {
			printf("No target watchpoint found!\n");
			assert(0);
		}
	}
}

