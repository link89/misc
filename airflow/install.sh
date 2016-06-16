# Evironment CentOS 6.5
# install python2.7 sqlite3 from source

# install airflow
pip install airflow

# workaround for backports.ssl_match_hostname-3.5.0.1.tar.gz not found
pip install https://pypi.python.org/packages/76/21/2dc61178a2038a5cb35d14b61467c6ac632791ed05131dda72c20e7b9e23/backports.ssl_match_hostname-3.5.0.1.tar.gz#md5=c03fc5e2c7b3da46b81acf5cbacfe1e6

# install mysql as backend
pip install airflow[mysql]

# install celery support for product env
pip install airflow[celery]
pip install celery[redis]

# workaround for import fail
pip install airflow[hive]

# if pysqlite install fail, run this
yum install mysql-devel

# In CentOS , if you saw pysqlite import _sqlite fail,
# try to compile libsqlite.so.0 from source,
# and replace the old one in /usr/lib64

./configure --prefix=/usr --disable-static        \
                     CFLAGS="-g -O2 -DSQLITE_ENABLE_FTS3=1 \
                     -DSQLITE_ENABLE_COLUMN_METADATA=1     \
                     -DSQLITE_ENABLE_UNLOCK_NOTIFY=1       \
                     -DSQLITE_SECURE_DELETE=1              \
                     -DSQLITE_ENABLE_DBSTAT_VTAB=1"
make
