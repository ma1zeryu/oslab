#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[])
{
    int even_only = 0;

    for (int i = 1; i < argc; i++)
    {
        if (strcmp(argv[i], "--even") == 0)
        {
            even_only = 1;
        }
    }

    char buf[256];
    while (fgets(buf, sizeof(buf), stdin) != NULL)
    {
        int x = atoi(buf);

        if (even_only)
        {
            if (x % 2 == 0)
            {
                printf("%d\n", x);
            }
        }
        else
        {
            printf("%d\n", x);
        }
    }

    fflush(stdout);
    return 0;
}