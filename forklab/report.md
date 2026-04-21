## PartA

```bash
yupeng@maizer:~/osclass$ ./fork_basic
[parent] pid=612, child=613
[child] pid=613, ppid=612
```

打印顺序理论上是不固定的

这里的pid本质上是tgid，是线程组（进程）的id

## PartB

```bash
yupeng@maizer:~/osclass/forklab$ ./fork_memory
[parent] x=93, &x=0x7ffe6f1f3c74, pid=687
[child ] x=123, &x=0x7ffe6f1f3c74, pid=688
```

我擦？虚拟地址一样但是x值不互相污染？

这是写时复制（COW，Copy-On-Write），如果有一方要写，内核就把对应的物理页复制一份，让写入方改自己的副本

## PartC

```bash
[parent] offset after write = 36
[child ] offset after write = 72
```

`fork_id_output.txt`

```bash
[P] parent pid=769, offset_before=0
[C] child pid=770, offset_before=36
```

父进程在 `open()` 后执行 `fork()`，父子进程均持有可用的文件描述符，但实验表明两者写文件时使用的是连续推进的统一偏移量，而不是各自独立从 0 开始。这说明父子继承的并不是两个独立的打开文件对象，而是各自文件描述符表中的条目共同指向同一个 open file description。

dup2知识新增了一个文件描述符，而不是创建新的open file description，不是“fd 号相同才共享”，而是“指向同一个 open file description 才共享”。

必须指向同一个open file description才能共享

`write`是原子的，所以不会导致写乱了

## PartD

```bash
ps -o pid,ppid,stat,cmd -p 1143,1144
    PID    PPID STAT CMD
   1143     368 S+   ./fork_basic
   1144    1143 Z+   [fork_basic] <defunct>
```

可见如果父进程长期旋转，子进程如果不主动回收就会变为僵尸

```bash
yupeng@maizer:~/osclass/forklab$ ps -o pid,ppid,stat,cmd -p 1173,1174
    PID    PPID STAT CMD
   1173    1002 S+   ./fork_basic
```

修改后（即添加waitpid收复子进程）可以恢复
