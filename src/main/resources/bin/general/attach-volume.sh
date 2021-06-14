#! /bin/bash

REGION=$(curl http://169.254.169.254/latest/meta-data/placement/region)
AVAILABILITY_ZONE=$(curl http://169.254.169.254/latest/meta-data/placement/availability-zone)
VOLUME_NAME_TAG=$(cat /home/ubuntu/volume-name-tag)
VOLUME_ID=$(aws ec2 describe-volumes --filters Name=tag:Name,Values=${VOLUME_NAME_TAG} Name=availability-zone,Values=${AVAILABILITY_ZONE} --region ${REGION} | jq -r '.Volumes[].VolumeId')

echo "Attaching ${VOLUME_ID}..." && aws ec2 attach-volume --device /dev/sdd \
--instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id) \
--volume-id ${VOLUME_ID} --region ${REGION} \
&& sleep 3

