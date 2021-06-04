#! /bin/bash

AWS_ROOT=/opt/aws

# Install cfn helper scripts
sudo mkdir -p ${AWS_ROOT}
sudo chown -R ubuntu:users ${AWS_ROOT}
curl https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-py3-latest.tar.gz --output /tmp/aws-cfn-bootstrap-py3-latest.tar.gz
tar -xvf /tmp/aws-cfn-bootstrap-py3-latest.tar.gz -C /tmp/
mv /tmp/aws-cfn-bootstrap-2.0/* ${AWS_ROOT}
cd /opt/aws
sudo python3 ${AWS_ROOT}/setup.py install --prefix ${AWS_ROOT} --install-lib /usr/lib/python3.8
chmod +x ${AWS_ROOT}/bin/*
sudo ln -s ${AWS_ROOT}/bin/cfn-hup /etc/init.d/cfn-hup