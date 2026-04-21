#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

int main(void)
{
    int fd = open("fork_fd_dup2.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644);
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
        int newfd = 100;
        if (dup2(fd, newfd) < 0)
        {
            perror("dup2");
            close(fd);
            _exit(1);
        }

        off_t off1 = lseek(newfd, 0, SEEK_CUR);
        dprintf(newfd, "[C via newfd] pid=%d, offset_before=%lld\n",
                getpid(), (long long)off1);

        off_t off2 = lseek(newfd, 0, SEEK_CUR);
        printf("[child ] newfd=%d, offset after write = %lld\n",
               newfd, (long long)off2);

        close(newfd);
        close(fd);
        _exit(0);
    }

    off_t off1 = lseek(fd, 0, SEEK_CUR);
    dprintf(fd, "[P via fd] pid=%d, offset_before=%lld\n",
            getpid(), (long long)off1);

    off_t off2 = lseek(fd, 0, SEEK_CUR);
    printf("[parent] fd=%d, offset after write = %lld\n",
           fd, (long long)off2);

    waitpid(pid, NULL, 0);
    close(fd);
    return 0;
}