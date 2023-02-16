#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)


char* my_itoa(int value) {
	int i = 0, k = 0, size = 0;;
	static char string[16];
	char* p = string;

	// negative number to positive number
	if(value >> 31) {
		value = ((~value) + 1);
		*p++ = '-';
	}

	// integer to string
	do {
		p[size++] = value % 10 + '0';
		value /= 10;
	} while(value > 0);

	p[size] = '\0';

	// reverse
	for(i = 0, k = size - 1; i < k; i++, k--) {
		p[i] ^= p[k];
		p[k] ^= p[i];
		p[i] ^= p[k];
	}

	return string;
}

int printf(const char *fmt, ...) {
  va_list ap;
  char *p, *sval;
  int ival;
  size_t len;

  size_t arg_cnt = 0;

  va_start(ap, fmt);
  for (p = (char *)fmt; *p; p++) {
    if (*p != '%') {
      putch(*p);
      continue;
    }
    ++p;
    int fix_num = 0;
    if (*p == '0') {
      ++p;
      char tmp = *p;
      if (tmp <= '9' && tmp >= '0')
        fix_num = tmp - '0';
      else
        panic("bad use!");
      ++p;
      while (*p <= '9' && *p >= '0') {
        fix_num = fix_num * 10 + (*p - '0');
        ++p;
      }
    }
    switch (*p) {
      case 'd':
        ival = va_arg(ap, int);
        char *str = my_itoa(ival);
        len = strlen(str);
        int rem = fix_num - len;
        if (rem > 0) {
          for (int i = 0; i < rem; i++) putch('0');
        }
        // todo
        for (int i = 0; i < len; i++) {
          putch(*str);
          str++;
        }
        arg_cnt += len;
        break;
      case 's':
        if (fix_num != 0) panic("bad use!");
        sval = va_arg(ap, char*);
        len = strlen(sval);
        // todo
        for (int i = 0; i < len; i++) {
          putch(*sval);
          sval++;
        }
        arg_cnt += len;
        break;
      default:
        panic("Not implemented");
    }
  }
  va_end(ap);
  return arg_cnt;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  char *s = out;
  va_list ap;
  char *p, *sval;
  int ival;
  size_t len;

  size_t arg_cnt = 0;

  va_start(ap, fmt);
  for (p = (char *)fmt; *p; p++) {
    if (*p != '%') {
      *s++ = *p;
      continue;
    }
    switch (*++p) {
      case 'd':
        ival = va_arg(ap, int);
        char *str = my_itoa(ival);
        len = strlen(str);
        strcpy(s, str);
        s += len;
        arg_cnt += len;
        break;
      case 's':
        sval = va_arg(ap, char*);
        len = strlen(sval);
        strcpy(s, sval);
        s += len;
        arg_cnt += len;
        break;
      default:
        panic("Not implemented");
    }
  }
  va_end(ap);

  *s = '\0';
  return arg_cnt;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
