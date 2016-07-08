import cx_Oracle

host = '127.0.0.1'
port = 1587
user = 'foo'
pswd = 'bar'
sid  = 'sid123'

# use makedsn to create dns
dsn = cx_Oracle.makedsn(host, port, sid)

# now you can create connection with user,pswd and dsn

with cx_Oracle.Connection(user, pswd, dsn) as conn:
    # do something here
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM tab01')
    for row in cursor:
        print 'Row:', row
