#include "syscall.h"

int
main()
{
	char *path = "open1.txt";
    open(path);
    //printf("%s\n", "open finished");
    return 0;
    /* not reached */
}
