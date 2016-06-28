# install deps
yum install zlib-devel libselinux-devel libuuid-devel pcre-devel openldap-devel lua-devel libxml2-devel openssl-devel libxslt-devel gd-devel perl-ExtUtils-Embed GeoIP-devel -y

export LUAJIT_LIB=/usr/local/lib
export LUAJIT_INC=/usr/local/include/luajit-2.0
make clean; CC=clang; CFLAGS="-g -O0" ./configure --enable-mods-static=all --with-ld-opt="-Wl,-rpath,/usr/local/lib" --with-debug --with-http_upstream_check_module --with-http_v2_module --with-http_dyups_module --with-http_dyups_lua_api --with-http_sysguard_module && make

ln -s /usr/local/nginx/sbin/nginx /usr/bin/nginx
