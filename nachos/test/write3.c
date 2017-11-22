#include "syscall.h"

int
main (int argc, char *argv[])
{
	int bigbuf1[20];
    file = "binary1.out";
    len = 10;  /* len in units of bytes, bigbufnum in ints */
    for (i = 0; i < 10; i++) {
	   bigbuf1[i] = i;
    }
    int fd = create(file);
    write(fd, bigbuf1, )

}
