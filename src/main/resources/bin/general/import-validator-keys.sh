#! /bin/bash

set -x

set -e

sudo mkdir -p /var/lib/lighthouse/validators && sudo chown ubuntu:ubuntu /var/lib/lighthouse/validators

mv $HOME/validator_keys/import.json $HOME/validator_keys/keystore-m_12381_3600_0_0_0-1632692812.json

lighthouse --network pyrmont account validator import --directory $HOME/validator_keys --datadir /var/lib/lighthouse