import gevent.monkey
gevent.monkey.patch_all()
import gevent
from gevent.pool import Pool

import pymysql
import random
import string
import time
import sys

HOST        = '127.0.0.1'
PORT        = 3306
USER        = 'root'
PASS        = ''
DB          = 'test01'

RAND_STR = ''.join([random.choice(string.ascii_letters + string.digits)
                    for n in xrange(1000)])

CREATE_STMT = \
'''
DROP TABLE IF EXISTS `test01`;
CREATE TABLE `test01`(
    `id` int(10) UNSIGNED NOT NULL,
    `state` tinyint NOT NULL DEFAULT 0,
    `others` varchar(1024) NOT NULL DEFAULT '',

    PRIMARY KEY (`id`),
    INDEX `idx_state` (`state`)
)  ENGINE=INNODB DEFAULT CHARSET=utf8;
'''

INSERT_STMT = ("INSERT INTO test01 SET "
               "id = {id}, state = {state}, others = '{others}'")

UPDATE_STMT = ("UPDATE test01 SET "
               "state = {state} WHERE id = {id}")

stats = []

def create_table():
    conn = pymysql.connect(host         = HOST,
                           port         = PORT,
                           user         = USER,
                           passwd       = PASS,
                           db           = DB,
                           autocommit   = True)
    cur = conn.cursor()
    cur.execute(CREATE_STMT)
    cur.close()
    conn.close()

def task(min_id, max_id):
    conn = pymysql.connect(host         = HOST,
                           port         = PORT,
                           user         = USER,
                           passwd       = PASS,
                           db           = DB,
                           autocommit   = True)
    cur = conn.cursor()
    for i in xrange(min_id, max_id):
        start_time = time.time()
        cur.execute(INSERT_STMT.format(id = i, state = 0, others = RAND_STR))
        stats.append(time.time() - start_time)

        start_time = time.time()
        cur.execute(UPDATE_STMT.format(id = i, state = 1))
        stats.append(time.time() - start_time)

        start_time = time.time()
        cur.execute(UPDATE_STMT.format(id = i, state = 2))
        stats.append(time.time() - start_time)

    cur.close()
    conn.close()

def main():
    # number of gevent worker
    conncurrent = int(sys.argv[1])
    # insert operation count of every worker
    # notice that every worker will perform 1 insert + 2 update
    # so the total operation every worker makes is size * 3
    size = int(sys.argv[2])

    create_table()

    args = [(i * size, (i + 1) * size) for i in xrange(conncurrent)]

    start_time = time.time()
    pool = Pool(conncurrent)
    pool.map(lambda arg: task(arg[0], arg[1]), args)
    elapsed_time = time.time() - start_time

    print "running time: {}".format(elapsed_time)
    print "total operations: {}".format(len(stats))
    print "max time: {}".format(min(stats))
    print "min time: {}".format(max(stats))
    print "total time: {}".format(sum(stats))
    print "average time (by total time): {}".format(sum(stats)/ len(stats))
    print "average time (by running time): {}".format(elapsed_time/ len(stats))

if __name__ == '__main__':
    main()
