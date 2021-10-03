#! /bin/bash

set -x

set -e

sudo useradd --no-create-home --shell /bin/false lighthousebeacon
sudo useradd --no-create-home --shell /bin/false lighthousevalidator
