#! /bin/bash

set -e

set -x

cat <<UNIT >> ./geth.service
[Unit]
Description=Ethereum go client
After=network.target 
Wants=network.target

[Service]
User=goeth 
Group=goeth
Type=simple
Restart=always
RestartSec=30

EnvironmentFile=-/etc/systemd/system/geth.service.env
ExecStartPre=/usr/local/bin/attach-goeth-volume.sh
ExecStartPre=+/usr/local/bin/format-goeth-volume.sh
ExecStartPre=+/usr/local/bin/mount-goeth-volume.sh

ExecStart=geth --goerli --http --http.addr "${GETH_HTTP_ADDR:-localhost}" --http.api net,eth,web3 --http.vhosts * --datadir /var/lib/goethereum

ExecStopPost=-+/usr/local/bin/unmount-goeth-volume.sh
ExecStopPost=-+/usr/local/bin/detach-goeth-volume.sh

[Install]
WantedBy=default.target
UNIT

sudo mv ./geth.service /etc/systemd/system/geth.service