# general runbooks

# create a volume
```bash
AZ=us-east-1a
SNAPSHOT="--snapshot-id snap-001799f517b01f531"
aws ec2 create-volume ${SNAPSHOT} \
--availability-zone ${AZ} \
--encrypted \
--iops 125 \
--size 150 \
--volume-type gp3 \
--tag-specification "ResourceType=volume,Tags=[{Key=Name,Value=goeth-lighthouse-data-${AZ}}]"
```

# attach a volume to an instance

```bash
INSTANCE_ID=i-061f2a6501545278c
VOLUME_ID=vol-0eae931e7fd7b40ce
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
SOURCE_DIR=/var/lib/backup/beacon
DEST_DIR=/var/lib/chaindata/lighthouse
sudo mkdir -p ${DEST_DIR}
echo "Syncing ${SOURCE_DIR} TO ${DEST_DIR}"
sudo rsync -aHAXxSP ${SOURCE_DIR} ${DEST_DIR} > /tmp/lighthouse-rsync.log 2>&1 &
```

## goethereum
```
SOURCE_DIR=/var/lib/goethereum/geth
DEST_DIR=/var/lib/backup/goethereum
sudo mkdir -p ${DEST_DIR}
echo "Syncing ${SOURCE_DIR} TO ${DEST_DIR}"
sudo rsync -aHAXxSP ${SOURCE_DIR} ${DEST_DIR} > /var/lib/backup/goethereum-rsync.log 2>&1 &
```