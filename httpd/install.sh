# update system
yum update -y

# build and install latest arp from source
yum install zlib-devel libselinux-devel libuuid-devel pcre-devel openldap-devel lua-devel libxml2-devel openssl-devel -y
wget http://mirror.bit.edu.cn/apache//apr/apr-1.5.2.tar.bz2
rpmbuild -tb apr-1.5.2.tar.bz2
yum localinstall rpmbuild/RPMS/x86_64/* -y

# For CentOS 7.0: install apr-util apr-util-devel from repo
yum install apr-util apr-util-devel -y

## For CentOS 6.5: install from source
#wget http://dev.mysql.com/get/mysql57-community-release-el6-8.noarch.rpm
#yum localinstall mysql57-community-release-el6-8.noarch.rpm -y
## NOTICE: enable 5.5 and disable 5.6 and 5.7
#
#yum install expat-devel postgresql-devel mysql-devel sqlite-devel freetds-devel unixODBC-devel nss-devel -y
#
#wget http://apache.fayea.com//apr/apr-util-1.5.4.tar.bz2
#rpmbuild -tb apr-util-1.5.4.tar.bz2
#yum localinstall rpmbuild/RPMS/x86_64/*

# build and install distcache from source
# alternative src: wget http://vault.centos.org/5.11/os/SRPMS/distcache-1.4.5-14.1.src.rpm
wget https://archive.fedoraproject.org/pub/archive/fedora/linux/releases/18/Everything/source/SRPMS/d/distcache-1.4.5-23.src.rpm
rpmbuild --rebuild --clean distcache-1.4.5-23.src.rpm
yum localinstall rpmbuild/RPMS/x86_64/*

# build and install apache httpd from source
wget http://mirrors.tuna.tsinghua.edu.cn/apache//httpd/httpd-2.4.20.tar.bz2
rpmbuild -tb httpd-2.4.20.tar.bz2
yum localinstall rpmbuild/RPMS/x86_64/*

# build and install php
# to workaroud apxs not found, create an soft link
ln -s /usr/bin/apxs /usr/sbin/apxs

yum install bzip2-devel curl-devel gmp-devel pam-devel libedit-devel libtool-ltdl-devel libc-client-devel firebird-devel net-snmp-devel libxslt-devel libjpeg-devel libpng-devel freetype-devel libXpm-devel t1lib-devel tokyocabinet-devel libmcrypt-devel libtidy-devel aspell-devel recode-devel libicu-devel enchant-devel
wget http://rpms.famillecollet.com/store/php/5.4.45/php-5.4.45-10.remi.src.rpm
rpmbuild --rebuild --clean php-5.4.45-10.remi.src.rpm
yum localinstall rpmbuild/RPMS/x86_64/*

# default it will use php-mysqlnd instead of mysql, if not, manually remove php-mysql and install php-mysqlnd
yum remove php-mysql
yum localinstall php-mysqlnd-5.4.16-36.1.el7.centos.1.x86_64.rpm

# edit httpd.conf, php-fpm config
