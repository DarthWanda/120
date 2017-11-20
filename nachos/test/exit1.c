/* 
 *  * exit1.c
 *   *
 *    * It does not get simpler than this...
 *     */
   
#include "syscall.h"

int
main (int argc, char *argv[])
{
    printf("begin exit1.c test ");
    exit (123);
    printf("never reach here");
}
