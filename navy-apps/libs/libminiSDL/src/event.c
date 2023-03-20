#include <NDL.h>
#include <SDL.h>
#include <string.h>

#define keyname(k) #k,

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  char buf[64];
  if (NDL_PollEvent(buf, sizeof(buf)) == 1) {
    char *keyname = strtok(buf, " ");
    int keycode = atoi(strtok(NULL, " "));
    int keydown = atoi(strtok(NULL, " "));

    if (keydown == 1) {
      event->type = SDL_KEYDOWN;
      switch (keycode) {
      }
    }
    else {
      event->type = SDL_KEYUP;
    }
  }
  
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return NULL;
}
