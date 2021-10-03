#! /bin/bash

set -e

set -x

LIGHTHOUSE_VERSION=v1.5.2

LIGHTHOUSE_ARCHIVE=lighthouse-${LIGHTHOUSE_VERSION}-x86_64-unknown-linux-gnu.tar.gz

LIGHTHOUSE_DOWNLOAD_URL=https://github.com/sigp/lighthouse/releases/download/${LIGHTHOUSE_VERSION}/${LIGHTHOUSE_ARCHIVE}

DOWNLOAD_DIR=/tmp

curl -L ${LIGHTHOUSE_DOWNLOAD_URL} --output ${DOWNLOAD_DIR}/${LIGHTHOUSE_ARCHIVE}

tar -xvf ${DOWNLOAD_DIR}/${LIGHTHOUSE_ARCHIVE} \
&& sudo mv lighthouse /usr/local/bin/lighthouse \
&& sudo chown lighthousebeacon:users /usr/local/bin/lighthouse
