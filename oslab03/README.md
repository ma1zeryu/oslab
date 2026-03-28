## os实验报告

本质：自己做一个简化版的shell pipeline

### PartA：实现3个”可串联“的程序

```c
//gen.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[]) {
    int n = 10;

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-n") == 0 && i + 1 < argc) {
            n = atoi(argv[i + 1]);
            i++;
        }
    }

    for (int i = 1; i <= n; i++) {
        printf("%d\n", i);
    }

    fflush(stdout);
    return 0;
}
//filter.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[]) {
    int even_only = 0;

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--even") == 0) {
            even_only = 1;
        }
    }

    char buf[256];
    while (fgets(buf, sizeof(buf), stdin) != NULL) {
        int x = atoi(buf);

        if (even_only) {
            if (x % 2 == 0) {
                printf("%d\n", x);
            }
        } else {
            printf("%d\n", x);
        }
    }

    fflush(stdout);
    return 0;
}
//stat.c
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

int main() {
    char buf[256];
    long long lines = 0;
    long long sum = 0;
    int min = INT_MAX;
    int max = INT_MIN;

    while (fgets(buf, sizeof(buf), stdin) != NULL) {
        int x = atoi(buf);
        lines++;
        sum += x;
        if (x < min) min = x;
        if (x > max) max = x;
    }

    if (lines == 0) {
        printf("lines=0\n");
    } else {
        printf("lines=%lld min=%d max=%d sum=%lld\n", lines, min, max, sum);
    }

    fflush(stdout);
    return 0;
}
```

先对三段程序编译，通过shell pipeline验证没问题

```bash
gcc gen.c -o gen
gcc filter.c -o filter
gcc stat.c -o stat

./gen -n 10 | ./filter --even | ./stat
```

![屏幕截图 2026-03-28 115943.png](https://picui.ogmua.cn/s1/2026/03/28/69c77df762bf6.webp)

### PartB：实现流水线执行器`plrun`

```c
//plrun.c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/wait.h>

static void die(const char *msg) {
    perror(msg);
    exit(1);
}

int main() {
    int pipe1[2];
    int pipe2[2];
    //创建两条管道
    if (pipe(pipe1) == -1) die("pipe1");
    if (pipe(pipe2) == -1) die("pipe2");

    pid_t pid1 = fork();
    if (pid1 < 0) die("fork gen");

    if (pid1 == 0) {
        // child 1: gen，把标准输出指到pipe1[1]
        if (dup2(pipe1[1], STDOUT_FILENO) == -1) die("dup2 gen stdout");

        close(pipe1[0]);
        close(pipe1[1]);
        close(pipe2[0]);
        close(pipe2[1]);

        char *args[] = {"./gen", "-n", "100", NULL};
        //替换子进程为gen程序
        execvp(args[0], args);

        perror("execvp gen");
        exit(1);
    }

    pid_t pid2 = fork();
    if (pid2 < 0) die("fork filter");

    if (pid2 == 0) {
        // child 2: filter，读从pipe1读，写往pipe2写
        if (dup2(pipe1[0], STDIN_FILENO) == -1) die("dup2 filter stdin");
        if (dup2(pipe2[1], STDOUT_FILENO) == -1) die("dup2 filter stdout");

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
    if (pid3 < 0) die("fork stat");

    if (pid3 == 0) {
        // child 3: stat
        if (dup2(pipe2[0], STDIN_FILENO) == -1) die("dup2 stat stdin");

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

    //回收子进程
    int status;
    waitpid(pid1, &status, 0);
    waitpid(pid2, &status, 0);
    waitpid(pid3, &status, 0);

    return 0;
}
```

![屏幕截图 2026-03-28 122025.png](https://picui.ogmua.cn/s1/2026/03/28/69c77df6efd82.webp)

父进程为什么需要关闭写端，如果父进程还保留着某个写段，下游进程就会认为还有人可能继续写，于是读不到EOF，被卡住

### PartC：观察与分析

为了观察进程，我们在`filter.c`中添加`sleep(10)`来避免快速结束，同时构建`rebuild.sh`脚本来一键重新编译所有文件

![屏幕截图 2026-03-28 132525.png](https://picui.ogmua.cn/s1/2026/03/28/69c77df85b515.webp)

`gen/filter/stat`的PPID都是`plrun`因为他们都是`plrun`fork出的子进程，

为什么这是进程树的一部分，因为整个系统所有进程构成一棵更大的父子关系树，这只是一个局部

`plrun pid=1560, gen=1561, filter=1562, stat=1563`

![屏幕截图 2026-03-28 133543.png](https://picui.ogmua.cn/s1/2026/03/28/69c77e03939ca.webp)

可以看到：

+ `gen`的`fd 1`标准输出被`dup2(pipe1[1], 1)`重定向到了第一条管道写端
+ `filter`的`fd 0`被重定向到第一条管道读端，`fd 1`被重定向到第二条管道写端
+ `stat`的`fd 0`被重定向到第二条管道读端
+ 父进程关闭不用的管道端后，不再参与数据流转，只负责等待和回收子进程

### PartD：故障复现

我们先尝试不关闭父进程的`pipe2`写端

发现他把`stat`和自己阻塞掉了

![屏幕截图 2026-03-28 134218.png](https://picui.ogmua.cn/s1/2026/03/28/69c77e03940db.webp)

接下来我们再尝试仅不关闭filter中的写端，看看会怎样

wooo，看来没关系，可见子进程关闭管道只是为了

不对，神秘的事情发生了

![屏幕截图 2026-03-28 135253.png](https://picui.ogmua.cn/s1/2026/03/28/69c77e03a4c22.webp)

哇我们好像发现了一个规律，这个关闭管道是否会阻塞是看进程之间的前序关系的，如果fliter不关闭pipe2的写端就没事，因为filter自己执行完了，pipe2的写端就自己关了，但如果filter不关闭pipe1的写端，就出现循环依赖了，pipe1的写端不关，gen就会被卡住，所以pipe1的写端就无法释放，所以filter也被卡住了，两个人互相把对方卡住了，就全gg了

嗯所以我们得出结论，上游的写端FD一定要关，他们彼此之间有绑定关系

修复的话，就把注释删掉就好了

把`rebuild.sh`更新为`Makefile`

```makefile
CC=gcc
CFLAGS=-Wall -Wextra -O2

all: gen filter stat plrun

gen: gen.c
	$(CC) $(CFLAGS) gen.c -o gen

filter: filter.c
	$(CC) $(CFLAGS) filter.c -o filter

stat: stat.c
	$(CC) $(CFLAGS) stat.c -o stat

plrun: plrun.c
	$(CC) $(CFLAGS) plrun.c -o plrun

clean:
	rm -f gen filter stat plrun
```

### Part E: IPC选型对比

IPC(Inter-Process Communication，进程间通信)

上面的通信方式被称为`pipe`匿名管道，本实验符合pipe的典型使用场景

+ 单向传输、顺序处理、前一个进程的输出作为下一个进程的输入

是shell里这条命令的底层逻辑，`gen | filter | stat`

只能用于父子进程或兄弟进程（找共同祖先），只能通过`fork`继承FD来使用

**FIFO（命名管道）**

有路径名，所以本来**没有亲缘关系**的进程也可以通过这个路径通信

对于 FIFO：

- `open(fifo, O_RDONLY)`
	 通常会阻塞，直到有写端打开
- `open(fifo, O_WRONLY)`
	 通常会阻塞，直到有读端打开

所以它比匿名 pipe 更依赖**启动顺序**和“对端是否存在”。

如果另一端没准备好，会卡，两边都准备好后才继续

启动顺序更值得关注，仍然适合单向流式传输，但管理成本比匿名管道略高

**Unix Domain Socket**

双向交互/请求响应

他天生设计适合双向，所以必然消耗更大，在当前场景下必然比不过为单向设计的pipe

**共享内存**

难度更高，因为`pipe/FIFO/UDS`都属于内核帮你维护数据流，而共享内存需要自己维护好安全的读写，避免读一半新数据，一半旧数据，覆盖，竞争等情况，需要通过信号量、互斥锁、条件变量、自旋等待标志位等来维护

但速度是最快的，遥遥领先这一块
