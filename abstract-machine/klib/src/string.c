#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t n;
  for (n = 0; *s != '\0'; s++) {
    n ++;
  }
  return n;
}

char *strcpy(char *dst, const char *src) {
  char *cp = dst;

  while ((*cp++ = *src++) != '\0')
    ;

  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  char* start = dst;

  while (n && (*dst++ = *src++))
    n--;

  if(n) {
    while (--n)
      *dst++ = '\0';
  }

  return(start);
}

char *strcat(char *dst, const char *src) {
  char *tmp = dst;

  while (*dst)
    dst++;
  while ((*dst++ = *src++) != '\0')
    ;

  return tmp;
}

int strcmp(const char *s1, const char *s2) {

  const unsigned char *p1 = (const unsigned char *) s1;
  const unsigned char *p2 = (const unsigned char *) s2;
  unsigned char c1, c2;
 
  do {
      c1 = (unsigned char) *p1++;
      c2 = (unsigned char) *p2++;
      if (c1 == '\0')
	      return c1 - c2;
  } while (c1 == c2);
 
  return c1 - c2;
}

int strncmp(const char *s1, const char *s2, size_t n) {
  const unsigned char *p1 = (const unsigned char *) s1;
  const unsigned char *p2 = (const unsigned char *) s2;
  unsigned char c1, c2;

  while (n > 0)
  {
    c1 = (unsigned char) *p1++;
    c2 = (unsigned char) *p2++;
    if (c1 == '\0' || c1 != c2)
      return c1 - c2;
    n--;
  }

  return 0;
}

void *memset(void *s, int c, size_t n) {
  char *xs = s;

  while (n--)
    *xs++ = c;
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  char *d = dst;
  char *s = (char *)src;
  if (d < s) {
    while (n--)
      *d++ = *s++;
  }
  else {
    // 当s的位置在d之前的话，需要考虑覆盖问题，
    char *lasts = s + (n - 1);
    char *lastd = d + (n - 1);
    while (n--)
      *lastd-- = *lasts--;
  }
  return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  char *d = out;
  const char *s = in;
  while (n--)
    *d++ = *s++;
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  const unsigned char *p1 = s1;
  const unsigned char *p2 = s2;

  while (n-- > 0) {
    if (*p1++ != *p2++)
      return p1[-1] < p2[-1] ? -1 : 1; // 比较之前所在的位置
  }
  return 0;
}

#endif
