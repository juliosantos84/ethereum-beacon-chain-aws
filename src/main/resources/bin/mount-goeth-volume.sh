#! /bin/bash

# Mount the volume
echo "Mounting goeth volume..." && sudo mkdir -p /var/lib/goethereum \
&& sudo mount /dev/nvme1n1 /var/lib/goethereum \
&& sudo chown -R goeth:goeth /var/lib/goethereum

