# ethereum-beacon-chain-aws
Code to operate ethereum beacon chain on AWS.  My design goal was to simplify the deployment, run lean and automate operations as much as possible so I can spend as little time as possible managing the node.

This configuration is based on [this](https://someresat.medium.com/guide-to-staking-on-ethereum-2-0-ubuntu-pyrmont-lighthouse-a634d3b87393) guide for deploying on Pyrmont on a PC and adapted for AWS with all steps automated (minus validator key import).  Shout to that dude for putting that guide together.

Feel free to use or modify as you see fit, and I'll gladly review pull requests for improvements.

If you want to thank me, you can send me some ETH: `0xf1De20b301eB00db8E491B567dE3149799692196` 😉

## Architecture
A single-node, single-AZ deployment that runs geth and lighthouse services.

## Resiliency/Redundancy
An autoscaling group ensures we have at least 1 instance running at all times and a single-attach EBS volume persists chaindata across instance replacements.  If multiple instances are running, only one can acquire the storage and provide attestations/proposals.  This simple approach avoids accidental slashings.

## Observability
Observability is handled by a combination of AWS CloudWatch and the use of a beaconcha.in monitoring endpoint.  

This allows for the creation of CloudWatch dashboards and use of the beaconcha.in mobile app to track the validator status and get application-level metrics and notifications (i.e. missed attestations).

CloudWatch infrastructure alerts (high/low cpu, high memory) can be configured to send notifications to an email and additional apps can be built to respond to alert notifications via SNS.

Confirmed working on goerli+pyrmont and mainnet.

## Deployments
### Requirements
1. An AWS account bootstrapped with `cdk bootstrap`
2. The etherythingbiig AMI built with [aws-imagepipelines](https://github.com/juliosantos84/aws-imagepipelines)
3. Configured `cdk.json`. [optional]

### Deploying
```bash
BEACON_CHAIN_NETWORK="mainnet"
bin/deploy-stacks.sh
```

To deploy individual stacks:
```bash
BEACON_CHAIN_NETWORK="mainnet"
CDK_DEPLOY_STACK="ethereumBeaconChainService/goeth" 
bin/deploy-stacks.sh
```

After deploying, scale up the ASGs.  You can run 
```bash
export ASG_NAME=<your asg name> 
export ASG_DESIRED_CAPACITY=1 
bin/set-desired-capacity.sh
```
## Todo
- Add healthcheck/watchdog to replace "bad" instances.
- Automate validator import.
