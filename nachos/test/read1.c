#include "syscall.h"

int
main (int argc, char *argv[])
{
    char buf[200];
    read(0, buf, 5);
    return 0;
}
