#! /bin/bash

set -e

set -x

sudo mv /tmp/bin/*.sh /usr/local/bin/ \
&& sudo chown lighthousebeacon:lighthousebeacon /usr/local/bin/*-volume.sh  \
&& sudo chmod 755 /usr/local/bin/*-volume.sh