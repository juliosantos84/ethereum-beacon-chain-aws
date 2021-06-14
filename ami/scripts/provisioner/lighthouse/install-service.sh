#! /bin/bash

cat <<UNIT >> ./lighthouse.service
[Unit]
Description=Lighthouse Beacon Node Service
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=lighthousebeacon
Group=lighthousebeacon
Restart=always
RestartSec=30

ExecStartPre=/usr/local/bin/attach-volume.sh
ExecStartPre=+/usr/local/bin/format-volume.sh
ExecStartPre=+/usr/local/bin/mount-volume.sh

ExecStart=/usr/local/bin/lighthouse beacon_node --datadir /var/lib/lighthouse --network pyrmont --staking --eth1 --eth1-endpoints http://goeth.private.ethereum.everythingbiig.com:8545 --metrics

ExecStopPost=-+/usr/local/bin/unmount-volume.sh
ExecStopPost=-+/usr/local/bin/detach-volume.sh

[Install]
WantedBy=multi-user.target
UNIT

sudo mv ./lighthouse.service /etc/systemd/system/lighthouse.service