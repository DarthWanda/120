#include "syscall.h"

int
main()
{
	int fd = 1;
    close(fd);
    return 0;
    /* not reached */
}
