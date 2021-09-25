#! /bin/bash

set -e

set -x

SOURCE_ROOT="ami"
SOURCE_FILES="goeth.pkr.hcl lighthouse.pkr.hcl"

for SOURCE_FILE in $SOURCE_FILES; do

    SRC_PATH=${SOURCE_ROOT}/${SOURCE_FILE}

    echo "Validating ${SRC_PATH}" && packer validate ${SRC_PATH}

    echo "Building ${SRC_PATH}" && packer build ${SRC_PATH}

done