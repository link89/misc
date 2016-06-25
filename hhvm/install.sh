# CentOS 7.0

# update packages
yum update -y

# install php

yum install php-mysql php-mcrypt php-xml php-cli php-soap php-gd php-ldap
yum install graphviz

# isstall dep
rpm -Uvh http://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-7.noarch.rpm

# remove libyaml if conflict is detected
yum remove libyaml-0.1.6-1.el6.x86_64

yum install cpp gcc-c++ cmake git psmisc {binutils,boost,jemalloc,numactl}-devel \
{ImageMagick,sqlite,tbb,bzip2,openldap,readline,elfutils-libelf,gmp,lz4,pcre}-devel \
lib{xslt,event,yaml,vpx,png,zip,icu,mcrypt,memcached,cap,dwarf}-devel \
{unixODBC,expat,mariadb}-devel lib{edit,curl,xml2,xslt}-devel \
glog-devel oniguruma-devel ocaml gperf enca libjpeg-turbo-devel openssl-devel \
mariadb mariadb-server zeromq-devel make -y

# install hhvm
wget http://mirrors.linuxeye.com/hhvm-repo/7/x86_64/hhvm-3.12.1-1.el7.centos.x86_64.rpm
yum localinstall hhvm-3.12.1-1.el7.centos.x86_64.rpm

# install apache
yum install httpd

# config firewall
firewall-cmd --add-port=80/tcp --permanent
firewall-cmd --reload
