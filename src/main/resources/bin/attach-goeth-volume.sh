#! /bin/bash

echo "Attaching goeth volume..." && aws ec2 attach-volume --device /dev/sdd \
--instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id) \
--volume-id $(cat /home/ubuntu/eth-volume-id) --region $(cat /home/ubuntu/eth-volume-region) \
&& sleep 3
