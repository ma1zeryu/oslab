// plrun.c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/wait.h>

static void die(const char *msg)
{
    perror(msg);
    exit(1);
}

int main()
{
    int pipe1[2];
    int pipe2[2];
    // 创建两条管道
    if (pipe(pipe1) == -1)
        die("pipe1");
    if (pipe(pipe2) == -1)
        die("pipe2");

    pid_t pid1 = fork();
    if (pid1 < 0)
        die("fork gen");

    if (pid1 == 0)
    {
        // child 1: gen，把标准输出指到pipe1[1]
        if (dup2(pipe1[1], STDOUT_FILENO) == -1)
            die("dup2 gen stdout");

        close(pipe1[0]);
        close(pipe1[1]);
        close(pipe2[0]);
        close(pipe2[1]);

        char *args[] = {"./gen", "-n", "100", NULL};
        // 替换子进程为gen程序
        execvp(args[0], args);

        perror("execvp gen");
        exit(1);
    }

    pid_t pid2 = fork();
    if (pid2 < 0)
        die("fork filter");

    if (pid2 == 0)
    {
        // child 2: filter，读从pipe1读，写往pipe2写
        if (dup2(pipe1[0], STDIN_FILENO) == -1)
            die("dup2 filter stdin");
        if (dup2(pipe2[1], STDOUT_FILENO) == -1)
            die("dup2 filter stdout");

        close(pipe1[0]);
        close(pipe1[1]);
        close(pipe2[0]);
        close(pipe2[1]);

        char *args[] = {"./filter", "--even", NULL};
        execvp(args[0], args);

        perror("execvp filter");
        exit(1);
    }

    pid_t pid3 = fork();
    if (pid3 < 0)
        die("fork stat");

    if (pid3 == 0)
    {
        // child 3: stat
        if (dup2(pipe2[0], STDIN_FILENO) == -1)
            die("dup2 stat stdin");

        close(pipe1[0]);
        close(pipe1[1]);
        close(pipe2[0]);
        close(pipe2[1]);

        char *args[] = {"./stat", NULL};
        execvp(args[0], args);

        perror("execvp stat");
        exit(1);
    }

    // parent
    close(pipe1[0]);
    close(pipe1[1]);
    close(pipe2[0]);
    close(pipe2[1]);

    // 回收子进程
    int status;
    waitpid(pid1, &status, 0);
    waitpid(pid2, &status, 0);
    waitpid(pid3, &status, 0);

    return 0;
}