#include "syscall.h"

int
main (int argc, char *argv[])
{
    bool status = unlink("open2.txt");
    printf("unlink status is:  %d\n", status);
    return 0;
}
