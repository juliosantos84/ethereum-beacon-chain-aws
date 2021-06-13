#! /bin/bash

# Format the file system
echo "Checking volume..." \
&& sudo file -s /dev/nvme1n1 | grep 'ext4' >> /dev/null \
|| sudo mkfs -t ext4 /dev/nvme1n1
