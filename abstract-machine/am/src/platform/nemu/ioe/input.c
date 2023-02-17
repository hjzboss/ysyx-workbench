#include <am.h>
#include <nemu.h>
#include <klib.h>

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  int keycode = (uint32_t)inl(KBD_ADDR);
  //printf("keycode=%d\n", keycode);
  kbd->keydown = 0;
  kbd->keycode = keycode;
}
