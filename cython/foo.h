#ifndef _FOO_H_
#define _FOO_H_

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct {
    int a;
    int b;
    char ip[32];
} Foo;

Foo ctor(int a, int b);
int sum1(Foo x);
int sum2(Foo* x);
const char* c_string(void);
void print_string(const char *str);

#ifdef __cplusplus
}
#endif

#endif
