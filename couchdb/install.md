yum install zip icu libicu-devel

wget https://packages.erlang-solutions.com/erlang/esl-erlang/FLAVOUR_1_general/esl-erlang_16.b.3-1~centos~6_amd64.rpm
yum localinstall esl-erlang_16.b.3-1~centos~6_amd64.rpm 

wget http://ftp.mozilla.org/pub/mozilla.org/js/js185-1.0.0.tar.gz
tar -xzvf js185-1.0.0.tar.gz 
cd js-1.8.5/
cd js/src/
./configure 
make
make install

wget http://apache.opencas.org/couchdb/source/1.6.1/apache-couchdb-1.6.1.tar.gz
tar -xzvf apache-couchdb-1.6.1.tar.gz 
cd apache-couchdb-1.6.1
./configure 
make
make install

adduser --no-create-home couchdb
chown -R couchdb:couchdb /usr/local/var/lib/couchdb /usr/local/var/log/couchdb /usr/local/var/run/couchdb
ln -sf /usr/local/etc/rc.d/couchdb /etc/init.d/couchdb
chkconfig --add couchdb
chkconfig couchdb on

vim /usr/local/etc/couchdb/local.ini
	port = 5984
	bind_address = 0.0.0.0

service couchdb start
curl 127.0.0.1:5984 -v
