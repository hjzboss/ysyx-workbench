#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)


// 将数字num转换为字符串str，基于基数base，目前不支持负数转换
void num2str(char* str, uint64_t num, int base) {
  char tmp[32];
  int len = 0;
  if (num == 0) tmp[len++] = 0;
  else {
    while (num) {
      tmp[len++] = num % base;
      num = num / base;
    }
  } 
  int i = 0;
  while (len-- > 0) {
    if (tmp[len] < 10) str[i] = tmp[len] + '0';
    else str[i] = tmp[len] - 10 + 'A';
    i++;
  }
  str[i] = '\0';
}


int printf(const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  char buf[20000]; // TODO：buf空间能否动态申请？
  int arg_cnt = vsprintf(buf, fmt, ap);
  va_end(ap);
  putstr(buf);
  return arg_cnt;
}

// 支持%d, %c, %u, %x, %s和%p，支持填充0
int vsprintf(char *out, const char *fmt, va_list ap) {
  char *p, *sval;
  int ival;
  unsigned long ptr;
  unsigned uval;
  int len;
  int arg_cnt = 0;
  char str[32];
  for (p = (char *)fmt; *p; p++) {
    if (*p != '%') {
      *out++ = *p;
      continue;
    }
    ++p;
    // 1. 填充计算
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
    // 2. 空格与0填充和占位符转换
    int fix_ch;
    int rem;
    switch (*p) {
      case 'c':
      case 'd':
        ival = va_arg(ap, int);
        num2str(str, ival, 10);
        len = strlen(str);
        rem = fix_num - len;
        if (rem > 0) {
          fix_ch = fix_zero ? '0' : ' ';
          for (; rem; rem--) *out++ = fix_ch;
        }
        // todo
        strcpy(out, str);
        out += len;
        arg_cnt += len;
        break;
      case 'u':
        uval = va_arg(ap, unsigned);
        num2str(str, uval, 10);
        len = strlen(str);
        rem = fix_num - len;
        if (rem > 0) {
          fix_ch = fix_zero ? '0' : ' ';
          for (; rem; rem--) *out++ = fix_ch;
        }
        // todo
        strcpy(out, str);
        out += len;
        arg_cnt += len;
        break;
      case 'p':
        ptr = va_arg(ap, unsigned long);
        num2str(str, ptr, 16);
        len = strlen(str);
        *out++ = '0';
        *out++ = 'x';
        rem = 16 - len;
        if (rem > 0) {
          for (; rem; rem--) *out++ = '0';
        }
        // todo
        strcpy(out, str);
        out += len;
        arg_cnt += len;
        break;
      case 'x':
        ival = va_arg(ap, int);
        num2str(str, ival, 16);
        len = strlen(str);
        rem = fix_num - len;
        if (rem > 0) {
          fix_ch = fix_zero ? '0' : ' ';
          for (; rem; rem--) *out++ = fix_ch;
        }
        // todo
        strcpy(out, str);
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
