# restore chaindata

#
1. From anywhere
```bash
INSTANCE_ID=i-0cc623cee412efa08
aws ec2 attach-volume --device /dev/sdf --instance-id ${INSTANCE_ID} --volume-id vol-0c3d94895dd5876fb
```

2. From $INSTANCE_ID
```bash
sudo file -s /dev/nvme2n1 | grep 'ext4' >> /dev/null \
|| sudo mkfs -t ext4 /dev/nvme2n1
```

3. From $INSTANCE_ID
`mkdir -p /var/lib/backup/goethereum && sudo mount /dev/nvme2n1 /var/lib/backup/goethereum`

4. From $INSTANCE_ID
sudo rsync -aHAXxSP /var/lib/backup/goethereum /var/lib/goethereum