#include "foo.h"
#include "stdio.h"

Foo ctor(int a, int b) {
    Foo x;
    x.a = 1;
    x.b = 2;
    return x;
}

int sum1(Foo x) {
    return x.a + x.b;
}

int sum2(Foo* x) {
    return x->a + x->b;
}
