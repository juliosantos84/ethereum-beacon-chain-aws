#! /bin/bash

VOLUME_MOUNT_PATH=$(cat /home/ubuntu/volume-mount-path)

# Unmount the file system
echo "Unmounting the volume..." && sudo umount ${VOLUME_MOUNT_PATH}
