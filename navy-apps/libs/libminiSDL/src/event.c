#include <NDL.h>
#include <SDL.h>
#include <string.h>
#include <stdlib.h>

#define keyname(k) #k,

static uint8_t key_state[83];

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  char buf[64];
  if (NDL_PollEvent(buf, sizeof(buf)) == 1) {
    char *keyname = strtok(buf, " ");
    int keycode = strtol(strtok(NULL, " "), NULL, 10);
    int keydown = strtol(strtok(NULL, " "), NULL, 10);
    ev->key.keysym.sym = keycode;
    ev->type = keydown == 1 ? SDL_KEYDOWN : SDL_KEYUP;
    if(ev->type == SDL_KEYDOWN) {
      key_state[keycode] = 1;
    }
    else {
      key_state[keycode] = 0;
    }
    return 1;
  }
  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  char buf[64];
  while (NDL_PollEvent(buf, sizeof(buf)) == 0);
  char *keyname = strtok(buf, " ");
  int keycode = strtol(strtok(NULL, " "), NULL, 10);
  int keydown = strtol(strtok(NULL, " "), NULL, 10);
  event->key.keysym.sym = keycode;
  event->type = keydown == 1 ? SDL_KEYDOWN : SDL_KEYUP;
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return key_state;
}
