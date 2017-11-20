#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *str[20];
    read(0, str, 10);
    write(1, str, 10);
    
    printf ("finished");
    return 0;
    printf ("finished");
}
