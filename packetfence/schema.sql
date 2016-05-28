CREATE TABLE acc_auth_reject (
    id int(10) NOT NULL auto_increment,
    username varchar(64) NOT NULL default '',
    calling_station_id varchar(50) NOT NULL default '',
    reject_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY user_and_station(username, calling_station_id),
    KEY reject_time(reject_time)
);
