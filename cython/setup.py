from distutils.core import setup
from distutils.extension import Extension
from Cython.Build import cythonize

ext_modules = cythonize([
    Extension("py_foo",
              sources=["py_foo.pyx"]
              )
])

setup(
    name='Demos',
    ext_modules=ext_modules,
)
