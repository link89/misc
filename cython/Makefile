all:
	gcc -c -fPIC foo.c -o foo.o
	gcc -shared -fPIC -o libfoo.so foo.o
clean:
	rm *.o *.so
