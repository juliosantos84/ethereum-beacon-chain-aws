#! /bin/bash

set -e

set -x

sudo add-apt-repository -y ppa:ethereum/ethereum

sudo apt update

sudo apt install geth -y