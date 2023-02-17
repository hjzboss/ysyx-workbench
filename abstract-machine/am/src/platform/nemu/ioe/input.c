#include <am.h>
#include <nemu.h>
#include <klib.h>

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  int keycode = (uint32_t)inl(KBD_ADDR);
  printf("shit\n");
  kbd->keydown = (keycode & KEYDOWN_MASK) == KEYDOWN_MASK ? 1 : 0;
  kbd->keycode = keycode ^ KEYDOWN_MASK;
}
