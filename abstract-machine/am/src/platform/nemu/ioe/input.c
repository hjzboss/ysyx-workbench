#include <am.h>
#include <nemu.h>
#include <klib.h>

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t keycode = (uint32_t)inl(KBD_ADDR);
  kbd->keydown = keycode & KEYDOWN_MASK ? true : false;
  if (kbd->keydown) {
    printf("keydown\n");
  }
  kbd->keycode = ((keycode | KEYDOWN_MASK) ^ KEYDOWN_MASK);
}
