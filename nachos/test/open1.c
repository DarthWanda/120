#include "syscall.h"

int
main()
{
	char *path = "open1.txt";
    open(path);
    /* not reached */
}
