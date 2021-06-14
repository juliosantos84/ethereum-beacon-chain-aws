#! /bin/bash

REGION=$(curl http://169.254.169.254/latest/meta-data/placement/region)
VOLUME_NAME_TAG=$(cat /home/ubuntu/volume-name-tag)
AVAILABILITY_ZONE=$(curl http://169.254.169.254/latest/meta-data/placement/availability-zone)
VOLUME_ID=$(aws ec2 describe-volumes --filters Name=tag:Name,Values=${VOLUME_NAME_TAG} Name=availability-zone,Values=${AVAILABILITY_ZONE} --region ${REGION} | jq -r '.Volumes[].VolumeId')
INSTANCE_ID=$(curl http://169.254.169.254/latest/meta-data/instance-id)

# Detach the volume
echo "Detaching the volume..." && aws ec2 detach-volume --region ${REGION} \
--device /dev/sdd --volume-id $VOLUME_ID \
--instance-id ${INSTANCE_ID}
