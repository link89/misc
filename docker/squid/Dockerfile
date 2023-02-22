FROM ubuntu:22.04

RUN apt update && \
    apt install -y squid openssl apache2-utils

ARG SQUID_CONF=/etc/squid/conf.d/my.conf
WORKDIR /squid
EXPOSE 4128

RUN echo 'https_port 4128 tls-cert=/squid/ca.pem ' > ${SQUID_CONF}  && \
    echo 'auth_param basic program /usr/lib/squid/basic_ncsa_auth /squid/passwords' >> ${SQUID_CONF} && \
    echo 'auth_param basic realm proxy' >> ${SQUID_CONF} && \
    echo 'acl authenticated proxy_auth REQUIRED' >> ${SQUID_CONF} && \
    echo 'http_access allow authenticated' >> ${SQUID_CONF} && \
    cat ${SQUID_CONF}
    

RUN echo 'set -e' > entrypoint.sh && \
    echo 'openssl req -new -newkey rsa:4096 -sha256 -days 365 -nodes -x509 -subj "/C=CN/ST=Fujian/L=Xiamen/O=SM/CN=link89.github.io" -keyout ca.pem -out ca.pem' >> entrypoint.sh && \
    echo 'htpasswd -b -c ./passwords $AUTH_USER $AUTH_PASS' >> entrypoint.sh && \
    echo 'squid -f /etc/squid/squid.conf -NYC' >> entrypoint.sh && \
    cat entrypoint.sh 
    

ENTRYPOINT [ "/bin/bash", "./entrypoint.sh" ]
