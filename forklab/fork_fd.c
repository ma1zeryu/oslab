#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <string.h>

int main(void)
{
    int fd = open("fork_fd_output.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0)
    {
        perror("open");
        return 1;
    }

    pid_t pid = fork();
    if (pid < 0)
    {
        perror("fork");
        close(fd);
        return 1;
    }

    if (pid == 0)
    {
        // 子进程
        off_t off_before = lseek(fd, 0, SEEK_CUR);
        dprintf(fd, "[C] child pid=%d, offset_before=%lld\n",
                getpid(), (long long)off_before);

        off_t off_after = lseek(fd, 0, SEEK_CUR);
        printf("[child ] offset after write = %lld\n", (long long)off_after);

        close(fd);
        _exit(0);
    }

    // 父进程
    off_t off_before = lseek(fd, 0, SEEK_CUR);
    dprintf(fd, "[P] parent pid=%d, offset_before=%lld\n",
            getpid(), (long long)off_before);

    off_t off_after = lseek(fd, 0, SEEK_CUR);
    printf("[parent] offset after write = %lld\n", (long long)off_after);

    waitpid(pid, NULL, 0);
    close(fd);
    return 0;
}