#! /bin/bash

set -e

set -x

sudo mv /tmp/bin/*.sh /usr/local/bin/ \
&& sudo chown goeth:goeth /usr/local/bin/*-goeth-volume.sh  \
&& sudo chmod 755 /usr/local/bin/*-goeth-volume.sh