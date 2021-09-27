#! /bin/bash

VOLUME_MOUNT_PATH=$(cat /home/ubuntu/volume-mount-path)
VOLUME_MOUNT_PATH_OWNER=$(cat /home/ubuntu/volume-mount-path-owner)

# Mount the volume
echo "Mounting goeth volume..." && sudo mkdir -p ${VOLUME_MOUNT_PATH} \
&& sudo mount /dev/nvme1n1 ${VOLUME_MOUNT_PATH} \
&& sudo chown -R ${VOLUME_MOUNT_PATH_OWNER}:${VOLUME_MOUNT_PATH_OWNER} ${VOLUME_MOUNT_PATH} \
&& sudo mkdir -p ${VOLUME_MOUNT_PATH}/validators \
&& sudo chown -R lighthousevalidator:lighthousevalidator ${VOLUME_MOUNT_PATH}/validators

