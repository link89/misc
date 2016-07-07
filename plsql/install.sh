# download
# instantclient-basic-macos.x64-12.1.0.2.0.zip
# instantclient-sdk-macos.x64-12.1.0.2.0.zip

# unzip and install
mkdir -p /opt/oracle
cd /opt/oracle
unzip instantclient-basic-macos.x64-12.1.0.2.0.zip
unzip instantclient-sdk-macos.x64-12.1.0.2.0.zip

export ORACLE_HOME=/opt/oracle/instantclient_12_1
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$ORACLE_HOME
# install python package
pip install cx_Oracle
