#!/usr/env/python
# -*- coding=utf-8 -*-

import pickle, functools, hashlib
import os.path

def sign(*args, **kwds):
    encoded = pickle.dumps((args, kwds))
    sha1 = hashlib.sha1(encoded)
    return sha1.hexdigest()

def try_cache(dir):
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwds):
            fname = os.path.join(dir, func.__name__ + '@' + sign(*args, **kwds) + '.pkl')
            try:
                with open(fname, 'r') as f:
                    ret = pickle.load(f)
            except:
                with open(fname, 'w') as f:
                    ret = func(*args, **kwds)
                    pickle.dump(ret, f)
            return ret
        return wrapper
    return decorator

def freeze(fname):
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwds):
            try:
                with open(fname, 'r') as f:
                    ret = pickle.load(f)
            except:
                with open(fname, 'w') as f:
                    ret = func(*args, **kwds)
                    pickle.dump(ret, f)
            return ret
        return wrapper
    return decorator

@try_cache('./')
def simple_return1(x):
    print 'run simple_return1'
    return x

@freeze('./freeze.pkl')
def simple_return2(x):
    print 'run simple_return2'
    return x

if __name__ == '__main__':
    print simple_return1(1000)
    # output:
    # run simple_return1
    # 1000
    print simple_return1(100)
    # output:
    # run simple_return1
    # 100
    print simple_return1(100)
    # output:
    # 100 (the result is read from cache file)

    print simple_return2(1000)
    # output:
    # run simple_return2
    # 1000
    print simple_return2(100)
    # output:
    # 1000 (the result is frozen in file ./freeze.pkl)
