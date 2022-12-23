#include <stdio.h>
#include <string.h>

typedef struct {
	int type;
	char str[32];
} token;

int main() {
	char buf[32];
	buf[2] = 'a';
	printf("%d\n", buf[2]);
	strcpy(buf, "32");
	printf("%d\n", buf[2]);
	return 0;
}
