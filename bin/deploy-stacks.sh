#! /bin/bash

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
export BEACON_CHAIN_NETWORK=${1:-"testnet"}
# export CDK_DEPLOY_ACCOUNT=""
# export CDK_DEPLOY_REGION=""
echo "Deploying beacon chain ${VERSION} on ${BEACON_CHAIN_NETWORK}"
cdk deploy --all --require-approval never