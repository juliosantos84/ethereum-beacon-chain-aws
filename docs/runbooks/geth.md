# geth runbooks

# restore chaindata
1. Attach the backup volume (from anywhere)
```bash
INSTANCE_ID=i-0cc623cee412efa08
BACKUP_VOLUME_ID=vol-0c3d94895dd5876fb
aws ec2 attach-volume --device /dev/sdf --instance-id ${INSTANCE_ID} --volume-id ${BACKUP_VOLUME_ID}
```

2. Mount the backup volume
`mkdir -p /var/lib/backup/goethereum && sudo mount /dev/nvme2n1 /var/lib/backup/goethereum`

3. Sync the backup to new volume
`sudo rsync -aHAXxSP /var/lib/backup/goethereum/goethereum /var/lib/goethereum`