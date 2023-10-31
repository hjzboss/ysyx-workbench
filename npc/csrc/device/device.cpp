#include <cpu/cpu.h>
#include <SDL2/SDL.h>


void init_vga();
void vga_update_screen();
void init_i8042();

void send_key(uint8_t, bool);
void vga_update_screen();

static uint64_t last = 0;

void device_update() {
  last++;
  if(last < UPDATE_FREQ) {
    return;
  }
  last = 0;
  vga_update_screen();

  SDL_Event event;
  while (SDL_PollEvent(&event)) {
    switch (event.type) {
      case SDL_QUIT:
        npc_state.state = NPC_QUIT;
        break;
      // If a key was pressed
      case SDL_KEYDOWN:
      case SDL_KEYUP: {
        uint8_t k = event.key.keysym.scancode;
        bool is_keydown = (event.key.type == SDL_KEYDOWN);
        send_key(k, is_keydown);
        break;
      }
      default: break;
    }
  }
}

void sdl_clear_event_queue() {
  SDL_Event event;
  while (SDL_PollEvent(&event));
}

void init_device() {
  init_vga();
  init_i8042();
}
