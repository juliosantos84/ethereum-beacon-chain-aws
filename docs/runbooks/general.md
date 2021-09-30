# general runbooks


# attach a volume to an instance

```bash
INSTANCE_ID=i-00b4c16c3b6a7b48b
VOLUME_ID=vol-02069c76474b2308c
aws ec2 attach-volume --device /dev/sdf --instance-id ${INSTANCE_ID} --volume-id ${VOLUME_ID}
 ```

 # format a second volume

 ```
 DEVICE_PATH=/dev/nvme2n1
 echo "Configuring ${DEVICE_PATH}"
 sudo file -s ${DEVICE_PATH} | grep 'ext4' >> /dev/null \
|| sudo mkfs -t ext4 $DEVICE_PATH
```

# mount a second volume for backups

```
DEVICE_PATH=/dev/nvme2n1
MOUNT_PATH=/var/lib/backup/
echo "Mounting ${DEVICE_PATH} to ${MOUNT_PATH}"
sudo mkdir -p ${MOUNT_PATH} && sudo mount ${DEVICE_PATH} ${MOUNT_PATH}
```

# sync a backup dir

## lighthouse
```
SOURCE_DIR=/var/lib/lighthouse/beacon
DEST_DIR=/var/lib/backup/lighthouse
sudo mkdir -p ${DEST_DIR}
echo "Syncing ${SOURCE_DIR} TO ${DEST_DIR}"
sudo rsync -aHAXxSP ${SOURCE_DIR} ${DEST_DIR} > /tmp/rsync.log 2>&1 &
```

## goethereum
```
SOURCE_DIR=/var/lib/goethereum/geth
DEST_DIR=/var/lib/backup/goethereum
sudo mkdir -p ${DEST_DIR}
echo "Syncing ${SOURCE_DIR} TO ${DEST_DIR}"
sudo rsync -aHAXxSP ${SOURCE_DIR} ${DEST_DIR} > /tmp/rsync.log 2>&1 &
```