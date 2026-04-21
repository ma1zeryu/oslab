#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

/**
 * @brief 主程序：fork 后分别修改同名变量，观察值与地址变化
 */
int main(void)
{
    int x = 100;
    pid_t pid = fork();
    if (pid < 0)
    {
        perror("fork");
        return 1;
    }

    if (pid == 0)
    {
        x += 23;
        printf("[child ] x=%d, &x=%p, pid=%d\n", x, (void *)&x, getpid());
        _exit(0);
    }

    x -= 7;
    printf("[parent] x=%d, &x=%p, pid=%d\n", x, (void *)&x, getpid());
    waitpid(pid, NULL, 0);
    return 0;
}