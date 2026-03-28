#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

int main()
{
    char buf[256];
    long long lines = 0;
    long long sum = 0;
    int min = INT_MAX;
    int max = INT_MIN;

    while (fgets(buf, sizeof(buf), stdin) != NULL)
    {
        int x = atoi(buf);
        lines++;
        sum += x;
        if (x < min)
            min = x;
        if (x > max)
            max = x;
    }

    if (lines == 0)
    {
        printf("lines=0\n");
    }
    else
    {
        printf("lines=%lld min=%d max=%d sum=%lld\n", lines, min, max, sum);
    }

    fflush(stdout);
    return 0;
}