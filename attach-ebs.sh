#! /bin/bash

DEVICE=/dev/sdd

INSTANCE_ID=$(curl http://169.254.169.254/latest/meta-data/instance-id)

VOLUME_ID=$(aws ec2 create-volume \
--size 20 \
--volume-type gp2 \
--availability-zone us-east-1 \
--output text \
--query 'VolumeId')

ec2 attach-volume
--device $DEVICE
--instance-id $INSTANCE_ID
--volume-id $VOLUME_ID