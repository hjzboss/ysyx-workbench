#ifndef __REG_H___
#define __REG_H__

#include <stdint.h>

void isa_reg_display(bool*);
uint64_t isa_reg_str2val(const char *s, bool *success);

#endif