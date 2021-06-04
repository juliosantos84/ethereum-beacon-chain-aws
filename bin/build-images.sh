#! /bin/bash

SOURCE_ROOT="ami"
SOURCE_FILES="lighthouse.pkr.hcl"

for SOURCE_FILE in $SOURCE_FILES; do

    SRC_PATH=${SOURCE_ROOT}/${SOURCE_FILE}

    echo "Validating ${SRC_PATH}" && packer validate ${SRC_PATH}

    echo "Building ${SRC_PATH}" && packer build ${SRC_PATH}

done