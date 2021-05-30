#! /bin/bash

# Make sure geth is stopped
sudo systemctl stop geth

# Unmount the file system
sudo umount /var/lib/goethereum

# Detach the volume
aws ec2 detach-volume --region us-east-1 --device /dev/nvme1n1 --volume-id $(cat /home/ubuntu/eth-volume-id) --instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id)