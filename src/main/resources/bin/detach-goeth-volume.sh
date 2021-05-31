#! /bin/bash

# Detach the volume
echo "Detaching the volume..." && aws ec2 detach-volume --region $(cat /home/ubuntu/eth-volume-region) \
--device /dev/sdd --volume-id $(cat /home/ubuntu/eth-volume-id) \
--instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id)