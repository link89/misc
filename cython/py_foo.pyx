# distutils: language = c
# distutils: sources = foo.c
# -*- coding: utf-8 -*-

from libc.stdio cimport FILE, fclose, fputs, fgets
from posix.stdio cimport fmemopen

cdef extern from "foo.h":
    ctypedef struct Foo:
        int a
        int b

    Foo ctor(int a, int b)
    int sum1(Foo x)
    int sum2(Foo *x)
    const char* c_string()
    void print_string(const char *str)

def py_ctor(int a, int b):
    return ctor(a, b)

def py_sum1(Foo x):
    return sum1(x)

# error: Cannot convert Python object argument to type 'Foo *'
# def py_sum2(Foo *x):
#     return sum2(x)

# it's wild that when running in OSX, an error will occur at module import
# >>> import py_foo
# Traceback (most recent call last):
#       File "<stdin>", line 1, in <module>
#       ImportError: dlopen(./py_foo.so, 2): Symbol not found: _fmemopen
#
# but it's ok at gnu/linux
def buffer_file():
    cdef char buffer[1024]
    cdef FILE* f = fmemopen(buffer, 1024, 'w')
    fputs("hello world", f)
    print(buffer)
    fclose(f)
    return buffer

def py_c_string():
    cdef bytes py_string = c_string()
    return py_string

# NOTICE the following function calls (test under python 2.7)
# py_print_str('123')       success
# py_print_str('你好')      success
# py_print_unicode(u'123')  success
# py_print_unicode(u'你好') fail

# it turns out that const char* in c is equivalent to bytes in cython
# so don't use unicode when you need call c functions
# I didn't test them in python 3, try bytearray when you have problems

def py_print_str(bytes py_string):
    print_string(py_string)

def py_print_unicode(unicode py_string):
    print_string(py_string)
