#ifndef _FOO_H_
#define _FOO_H_

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct {
    int a;
    int b;
} Foo;

Foo ctor(int a, int b);
int sum1(Foo x);
int sum2(Foo* x);

#ifdef __cplusplus
}
#endif

#endif
