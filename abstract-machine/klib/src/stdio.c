#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)


char* num2str(int num, int base) {
  static char str[16];
  char tmp[32];
  int len = 0;
  bool is_neg = false;
  if (num == 0) tmp[len++] = 0;
  else if(num >> 31) {
    // 负数
    num = ((~num) + 1);
    str[0] = '-';
    is_neg = true;
  }
  else while (num) {
    tmp[len++] = num % base;
    num = num / base;
  }
  // todo
  int i = is_neg ? 1 : 0;
  while (len-- > 0) {
    if (tmp[len] < 10) str[i] = tmp[len] + '0';
    else str[i] = tmp[len] - 10 + 'A';
    i++;
  }
  str[i] = '\0';
  return str;
}


int printf(const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  char buf[200];
  int arg_cnt = vsprintf(buf, fmt, ap);
  va_end(ap);
  putstr(buf);
  return arg_cnt;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  char *p, *sval;
  int ival;
  int len;
  int arg_cnt = 0;
  for (p = (char *)fmt; *p; p++) {
    if (*p != '%') {
      *out++ = *p;
      continue;
    }
    ++p;
    int fix_num = 0; // 填充0的数目
    bool fix_zero = false;
    // 判断是否要填充0
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
      fix_zero = true;
    }
    else if (*p <= '9' && *p > '0') {
      do {
        fix_num = fix_num * 10 + (*p - '0');
        ++p;
      } while (*p <= '9' && *p >= '0');
    }
    int fix_ch;
    int rem;
    switch (*p) {
      case 'c':
      case 'd':
        ival = va_arg(ap, int);
        char *str = num2str(ival, 10);
        len = strlen(str);
        rem = fix_num - len;
        if (rem > 0) {
          fix_ch = fix_zero ? '0' : ' ';
          for (int i = 0; i < rem; i++) *out++ = fix_ch;
        }
        // todo
        strcpy(out, str);
        out += len;
        arg_cnt += len;
        break;
      case 'x':
        ival = va_arg(ap, int);
        char *string = num2str(ival, 16);
        len = strlen(string);
        rem = fix_num - len;
        if (rem > 0) {
          fix_ch = fix_zero ? '0' : ' ';
          for (int i = 0; i < rem; i++) *out++ = fix_ch;
        }
        // todo
        strcpy(out, string);
        out += len;
        arg_cnt += len;
        break;
      case 's':
        if (fix_num != 0) panic("bad use!");
        sval = va_arg(ap, char*);
        len = strlen(sval);
        strcpy(out, sval);
        out += len;
        arg_cnt += len;
        break;
      default:
        putstr("fuck:");
        putch(*p);
        putch('\n');
        panic("Not implemented");
    }
  }
  *out = '\0';
  return arg_cnt;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int arg_cnt = vsprintf(out, fmt, ap);
  va_end(ap);
  return arg_cnt;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
