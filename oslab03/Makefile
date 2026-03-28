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