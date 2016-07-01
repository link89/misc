# install dependency

wget http://dev.mysql.com/get/mysql57-community-release-el6-8.noarch.rpm
yum localinstall mysql57-community-release-el6-8.noarch.rpm

yum install mysql mysql-server mysql-devel
yum install php php-mysql php-mcrypt php-xml php-cli php-soap php-gd php-ldap
yum install graphviz

# itop 2.3 beta has some bugs, use itop 2.2.1
cd /var/www/html/
unzip ~/iTop-2.2.1-2658.zip

# chown to apache
chown -R apache web

# NOTICE: when install on nginx, you should mannualy change
# app_root_url to domain name (default is localhost)
