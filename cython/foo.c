#include <stdio.h>
#include <string.h>
#include "foo.h"

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

const char* c_string() {
    return "this is a c string";
}

void print_string(const char* str) {
    printf("%s\n", str);
    return;
}

void print_foo(Foo x) {
    printf("%d, %d, %s\n", x.a, x.b, x.s);
    return;
}

void set_foo_s(Foo *x, const char *str) {
    strcpy(x->s, str);
    return;
}
