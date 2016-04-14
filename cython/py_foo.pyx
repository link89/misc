# distutils: language = c
# distutils: sources = foo.c

cdef extern from "foo.h":
    ctypedef struct Foo:
        int a
        int b

    Foo ctor(int a, int b)
    int sum1(Foo x)
    int sum2(Foo *x)

def py_ctor(int a, int b):
    return ctor(a, b)

def py_sum1(Foo x):
    return sum1(x)

# error: Cannot convert Python object argument to type 'Foo *'
# def py_sum2(Foo *x):
#     return sum2(x)
