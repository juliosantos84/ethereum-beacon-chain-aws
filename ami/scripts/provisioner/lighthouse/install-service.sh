#! /bin/bash

sudo cat <<UNIT >> /etc/systemd/system/lighthouse.service
[Unit]
Description=Lighthouse Beacon Node Service
Wants=network-online.target
After=network-online.target
[Service]
Type=simple
User=lighthousebeacon
Group=lighthousebeacon
Restart=always
RestartSec=5
ExecStart=/usr/local/bin/lighthouse beacon_node --datadir /var/lib/lighthouse --network pyrmont --staking --eth1-endpoint http://goeth.ethereum.everythingbiig.com:8545 --metrics
[Install]
WantedBy=multi-user.target
UNIT