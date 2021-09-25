#! /bin/bash

set -e

set -x

sudo add-apt-repository -y ppa:ethereum/ethereum

sudo apt update

sudo apt install geth=1.10.8+build27284+focal -y