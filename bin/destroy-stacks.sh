#! /bin/bash

# export CDK_DEPLOY_ACCOUNT="you account id"
# export CDK_DEPLOY_REGION="aws region"
# export CDK_DEPLOY_PROFILE_FLAG="--profile you-profile"

export CDK_DEFAULT_ACCOUNT=${CDK_DEPLOY_ACCOUNT:-"default account id"}
export CDK_DEFAULT_REGION=${CDK_DEPLOY_REGION:-"default region"}

cdk destroy --all --force ${CDK_DEPLOY_PROFILE_FLAG}