#include "syscall.h"

int
main (int argc, char *argv[])
{
    char* buf = malloc(10 * sizeof(char));
    read(0, buf, 5);
    return 0;
}
