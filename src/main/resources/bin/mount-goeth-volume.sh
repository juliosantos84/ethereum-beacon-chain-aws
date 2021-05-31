#! /bin/bash

# Mount the volume
echo "Mapping goeth volume..." && sudo mkdir -p /var/lib/goethereum \
&& sudo mount /dev/nvme1n1 /var/lib/goethereum
