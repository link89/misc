cdef extern from "foo.h":
    struct Foo:
        int a
        int b

    Foo ctor(int a, int b)
    int sum1(Foo x)
    int sum2(Foo *x)
