#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <config.h>
#include <SDL2/SDL.h>
#include <assert.h>


static uint32_t vmem_size;
static uint64_t vmem_upper_bound;

static uint32_t screen_width() {
  return SCREEN_W;
}

static uint32_t screen_height() {
  return SCREEN_H;
}

static uint8_t *vmem = NULL;
static uint32_t *vgactl_port_base = NULL;

static SDL_Renderer *renderer = NULL;
static SDL_Texture *texture = NULL;

static void init_screen() {
  SDL_Window *window = NULL;
  char title[128];
  sprintf(title, "riscv64-NPC");
  SDL_Init(SDL_INIT_VIDEO);
  SDL_CreateWindowAndRenderer(
      SCREEN_W * (MUXDEF(CONFIG_VGA_SIZE_400x300, 2, 1)),
      SCREEN_H * (MUXDEF(CONFIG_VGA_SIZE_400x300, 2, 1)),
      0, &window, &renderer);
  SDL_SetWindowTitle(window, title);
  texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888,
      SDL_TEXTUREACCESS_STATIC, SCREEN_W, SCREEN_H);
}

static inline void update_screen() {
  SDL_UpdateTexture(texture, NULL, vmem, SCREEN_W * sizeof(uint32_t));
  SDL_RenderClear(renderer);
  SDL_RenderCopy(renderer, texture, NULL, NULL);
  SDL_RenderPresent(renderer);
}

void vga_update_screen() {
  // TODO: call `update_screen()` when the sync register is non-zero,
  // then zero out the sync register
  if (vgactl_port_base[1] == 1) {
    update_screen();
    vgactl_port_base[1] = 0;
  }
}

bool check_vmem_bound(uint64_t addr) {
  return addr >= CONFIG_FB_ADDR && addr <= vmem_upper_bound;
}

uint32_t get_vga_config() {
  return vgactl_port_base[0];
}

uint64_t fb_read(uint64_t addr, int len) {
  uint64_t offset = addr - CONFIG_FB_ADDR;
  return *(uint64_t *)(vmem + offset);
}

void fb_write(uint64_t addr, int len, uint64_t data) {
  uint64_t offset = addr - CONFIG_FB_ADDR;
  *(uint64_t *)(vmem + offset) = data;
}

void init_vga() {
  vgactl_port_base = (uint32_t *)malloc(8);
  vgactl_port_base[0] = (screen_width() << 16) | screen_height();
  vmem_size = screen_width() * screen_height() * sizeof(uint32_t);
  vmem_upper_bound = CONFIG_FB_ADDR + vmem_size;
  vmem = (uint8_t *)malloc(vmem_size);
  init_screen();
  memset(vmem, 0, vmem_size);
}