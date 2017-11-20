#include "syscall.h"

int
main (int argc, char *argv[])
{
    int fd = creat("open1.txt");
    printf("%d\n", fd);
    return 0;
}
