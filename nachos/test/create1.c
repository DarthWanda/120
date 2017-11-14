#include "syscall.h"

int
main (int argc, char *argv[])
{
    int fd = create("open1.txt");
    printf("%d\n", fd);
}
