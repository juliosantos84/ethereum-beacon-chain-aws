# general runbooks


# attach a volume to an instance

```bash
INSTANCE_ID=i-0cc623cee412efa08
VOLUME_ID=vol-0db22afe7e1ab0c6b
aws ec2 attach-volume --device /dev/sdd --instance-id ${INSTANCE_ID} --volume-id ${VOLUME_ID}
 ```