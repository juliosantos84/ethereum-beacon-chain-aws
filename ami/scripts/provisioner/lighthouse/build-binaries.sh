#! /bin/bash

RUSTUP_SCRIPT=./rustup-install.sh
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs > ${RUSTUP_SCRIPT} \
&& sudo chmod u+x ${RUSTUP_SCRIPT} \
&& ${RUSTUP_SCRIPT} -y \
&& rm -rf ${RUSTUP_SCRIPT}

source $HOME/.cargo/env

sudo apt update && \
sudo apt install -y git gcc g++ make cmake pkg-config libssl-dev

git clone -b v1.3.0 https://github.com/sigp/lighthouse.git \
&& make -C lighthouse