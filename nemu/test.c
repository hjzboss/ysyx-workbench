#include <stdio.h>
#include <string.h>

typedef struct {
	int type;
	char str[32];
} token;

int main() {
	token a;
	char *fuck = "fuck";
	printf("%ld\n", strlen(fuck));
	strncpy(a.str, fuck, strlen(fuck));
	a.str[strlen(fuck)] = '\0';
	printf("%s\n", a.str);
	return 0;
}
