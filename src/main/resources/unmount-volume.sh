#! /bin/bash

# Make sure geth is stopped
echo "Stopping geth..." && sudo systemctl stop geth && sleep 3

# Unmount the file system
echo "Unmounting the volume..." && sudo umount /var/lib/goethereum

# Detach the volume
echo "Detaching the volume..." && aws ec2 detach-volume --region us-east-1 \
--device /dev/sdd --volume-id $(cat /home/ubuntu/eth-volume-id) \
--instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id)