#! /bin/bash

set -e

set -x

LIGHTHOUSE_VERSION=v1.3.0

LIGHTHOUSE_ARCHIVE=lighthouse-v1.3.0-x86_64-unknown-linux-gnu.tar.gz

LIGHTHOUSE_DOWNLOAD_URL=https://github.com/sigp/lighthouse/releases/download/v1.3.0/lighthouse-v1.3.0-x86_64-unknown-linux-gnu.tar.gz

LIGHTHOUSE_SIG_FILE=${LIGHTHOUSE_ARCHIVE}.asc

LIGHTHOUSE_SIGNATURE_URL=https://github.com/sigp/lighthouse/releases/download/v1.3.0/lighthouse-v1.3.0-x86_64-unknown-linux-gnu.tar.gz.asc

SIGMA_PRIME_PGP_KEY=15E66D941F697E28F49381F426416DC3F30674B0

DOWNLOAD_DIR=/tmp

curl -L ${LIGHTHOUSE_DOWNLOAD_URL} --output ${DOWNLOAD_DIR}/${LIGHTHOUSE_ARCHIVE}

curl -L ${LIGHTHOUSE_SIGNATURE_URL} --output ${DOWNLOAD_DIR}/${LIGHTHOUSE_SIG_FILE}

# verify 
# gpg --receive-keys ${SIGMA_PRIME_PGP_KEY} \
# && gpg --verify ${DOWNLOAD_DIR}/${LIGHTHOUSE_SIG_FILE} ${DOWNLOAD_DIR}/${LIGHTHOUSE_ARCHIVE}

tar -xvf ${DOWNLOAD_DIR}/${LIGHTHOUSE_ARCHIVE} \
&& sudo mv lighthouse /usr/local/bin/lighthouse \
&& sudo chown lighthousebeacon:users /usr/local/bin/lighthouse