#include "syscall.h"

int
main (int argc, char *argv[])
{
    int fd = creat("open2.txt");
    printf("%d\n", fd);
    return 0;
}
