# ethereum-beacon-chain-aws
Code to operate ethereum beacon chain on AWS.  

This deployment is based on [this](https://someresat.medium.com/guide-to-staking-on-ethereum-2-0-ubuntu-pyrmont-lighthouse-a634d3b87393) guide for deploying Pyrmont and adapted for AWS with all steps automated (minus validator key import).

## Architecture
A single-node, single AZ deployment that runs geth and lighthouse services.  Confirmed working on goerli+pyrmont and mainnet.

## Deployments
### Requirements
1. An AWS account bootstrapped with `cdk bootstrap`
2. The etherythingbiig AMI built with [aws-imagepipelines](https://github.com/juliosantos84/aws-imagepipelines)
3. Configured `cdk.json`. [optional]

### Deploying
```bash
BEACON_CHAIN_NETWORK="testnet"
bin/deploy-stacks.sh
```

To deploy individual stacks:
```bash
BEACON_CHAIN_NETWORK="testnet"
CDK_DEPLOY_STACK="ethereumBeaconChainService/development" 
bin/deploy-stacks.sh
```

After deploying, scale up the ASGs.  You can run 
```bash
export ASG_NAME=<your asg name> 
export ASG_DESIRED_CAPACITY=1 
bin/set-desired-capacity.sh
```
## Todo
[ ] Add healthcheck/watchdog to replace "bad" instances.
[ ] Automate validator import.
