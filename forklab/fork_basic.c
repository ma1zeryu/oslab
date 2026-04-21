#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

/**
 * @brief 主程序：调用 fork 并打印父子进程 PID/PPID 信息
 */
int main(void)
{
    pid_t pid = fork();
    if (pid < 0)
    {
        perror("fork");
        return 1;
    }

    if (pid == 0)
    {
        printf("[child] pid=%d, ppid=%d\n", getpid(), getppid());
        _exit(0);
    }

    printf("[parent] pid=%d, child=%d\n", getpid(), pid);
    waitpid(pid, NULL, 0);
    sleep(20);
    return 0;
}