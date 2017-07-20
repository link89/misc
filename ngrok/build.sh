set -e
# for building and server
cd $(dirname $0)

make clean
NGROK_DOMAIN="foo.com"
openssl genrsa -out rootCA.key 2048
openssl req -x509 -new -nodes -key rootCA.key -subj "/CN=$NGROK_DOMAIN" -days 5000 -out rootCA.pem
openssl genrsa -out device.key 2048
openssl req -new -key device.key -subj "/CN=$NGROK_DOMAIN" -out device.csr
openssl x509 -req -in device.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out device.crt -days 5000

cp rootCA.pem assets/client/tls/ngrokroot.crt
cp device.crt assets/server/tls/snakeoil.crt
cp device.key assets/server/tls/snakeoil.key

make release-server
make release-client

cp ./bin/ngrok /tmp/

# run server with
./bin/ngrokd -tlsKey="assets/server/tls/snakeoil.key" -tlsCrt="assets/server/tls/snakeoil.crt" -domain="foo.com"  -httpAddr=":8081" -httpsAddr=":8082" -tunnelAddr=":8083"

# for client
scp user@remote_ip_addr:/tmp/ngrok .
cat <<EOF > ~/.ngrok
server_addr: foo.com:8083
trust_host_root_certs: false

tunnels:
  mtyun:
    proto:
      http: 8080
    hostname: remote_ip_addr:8081
    log-level: debug
    log: stdout
EOF

# run client with
ngrok start mtyun

# and don't forget to modify hosts file with: remote_ip_addr foo.com
