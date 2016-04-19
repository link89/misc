all:
	gcc -c -fPIC foo.c -o foo.o
	gcc -shared -fPIC -o libfoo.so foo.o
	python setup.py build_ext --inplace
clean:
	rm *.o *.so
