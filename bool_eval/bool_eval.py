#!/usr/env/python
# -*- coding=utf-8 -*-

ops = {'and'    : all,
       'or'     : any,
       'not'    : lambda x: not x,
       'eq'     : lambda x, y: x == y,
       'ne'     : lambda x, y: x != y,
       'gt'     : lambda x, y: x > y,
       'ge'     : lambda x, y: x >= y,
       'lt'     : lambda x, y: x < y,
       'le'     : lambda x, y: x <= y
       }

def bool_eval(exp, obj, ops = ops):
    if isinstance(exp, list):
        op_name = exp[0]
        op = ops[op_name]

        if op_name in ('and', 'or'):
            return op(map(lambda x: bool_eval(x, obj, ops), exp[1:]))
        elif op_name in ('not'):
            return op(bool_eval(exp[1:], obj, ops))
        else:
            return op(obj[exp[1]],exp[2])
    else:
        return exp

if __name__ == '__main__':
    obj_list = [
        {'name'     : 'alice',
         'age'      : 10,
         'gender'   : 'female'},
        {'name'     : 'bob',
         'age'      : 20,
         'gender'   : 'male'},
        {'name'     : 'carol',
         'age'      : 30,
         'gender'   : 'female'},
        {'name'     : 'dave',
         'age'      : 40,
         'gender'   : 'female'},
    ]

    exp = ['and', ['eq', 'gender', 'female'], ['lt', 'age', 25]]

    print 'expected: [True, False, False, False]'
    print 'results: ', map(lambda o: bool_eval(exp, o), obj_list)
