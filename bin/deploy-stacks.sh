#! /bin/bash

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

export BEACON_CHAIN_NETWORK=${BEACON_CHAIN_NETWORK:-"testnet"}
# export CDK_DEPLOY_ACCOUNT=""
# export CDK_DEPLOY_REGION=""
# export CDK_DEPLOY_EXTRA_CONTEXT="-c everythingbiig-aws-imagepipelines/etherythingbiig:distributionRegion=us-east-1"
# export CDK_DEPLOY_PROFILE_FLAG="--profile your profile"
# export CDK_DEPLOY_STACK=""

echo "Deploying beacon chain ${VERSION} on ${BEACON_CHAIN_NETWORK}"
echo -e "Extra options:\n\t${CDK_DEPLOY_EXTRA_CONTEXT}"
cdk deploy ${CDK_DEPLOY_STACK:-"--all"} --require-approval never ${CDK_DEPLOY_PROFILE_FLAG} ${CDK_DEPLOY_EXTRA_CONTEXT}