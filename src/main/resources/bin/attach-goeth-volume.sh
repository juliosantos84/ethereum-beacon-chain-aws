#! /bin/bash

#REGION=$(curl http://169.254.169.254/latest/meta-data/region)
#AZ=$(curl http://169.254.169.254/latest/meta-data/availability-zone)
#VOLUME_ID=$(aws ec2 describe-volumes --filters Name=tag:Name,Values=chaindata Name=availability-zone,Values=us-east-1a)

echo "Attaching goeth volume..." && aws ec2 attach-volume --device /dev/sdd \
--instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id) \
--volume-id $(cat /home/ubuntu/eth-volume-id) --region $(cat /home/ubuntu/eth-volume-region) \
&& sleep 3
