	/*
 *  * exec1.c
 *   *
 *    * Simple program for testing exec.  It does not pass any arguments to
 *     * the child.
 *      */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog = "write10.coff";
    int pid;
    printf("%s\n", "ready to exec");
    pid = exec(prog, 0, 0);
    pid = exec(prog, 0, 0);
    pid = exec(prog, 0, 0);
    printf("%s\n", "finish  exec");
    printf("pid num is: %d\n", pid);
    // the exit status of this process is the pid of the child process
    exit (pid);
}
