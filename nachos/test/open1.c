#include "syscall.h"

int
main()
{
	char *path = "open1.txt";
    open(path);
    return 0;
    /* not reached */
}
