# ethereum-beacon-chain-aws
Code to operate ethereum beacon chain on AWS.  My design goal was to simplify the deployment, run lean and automate operations as much as possible so I can spend as little time as possible managing the node.

This configuration is based on [this](https://someresat.medium.com/guide-to-staking-on-ethereum-2-0-ubuntu-pyrmont-lighthouse-a634d3b87393) guide for deploying on Pyrmont on a PC and adapted for AWS with all steps automated (minus validator key import).  Shout to that dude for putting that guide together.

Feel free to use or modify as you see fit, and I'll gladly review pull requests for improvements.

If my work saves you time and you want to thank me, you can send me some ETH: `0xf1De20b301eB00db8E491B567dE3149799692196` 😉

## Architecture
A single-node, single-AZ deployment that runs geth and lighthouse services.

### Availability
An autoscaling group ensures we have at least 1 instance running at all times and a single-attach EBS volume persists chaindata across instance replacements.  If multiple instances are running, only one can acquire the storage and provide attestations/proposals.  This simple approach avoids accidental slashings.  Live deployments can be performed and the autoscaling group will replace instances in 3-6 minutes.  This often leads to a missed attestation, but no reward penalty.

### Reliability
All application services are designed to be modular and fail gracefully without bringing down the whole app.
- The volume attachment and mounting services run until they can successfully acquire the chaindata volume.
- Geth, Lighthouse beacon and validator services have no hard dependencies on each other.
- Lighthouse's eth1 endpoints can be configured with additional fallbacks (i.e. infura.io).
- Geth (the biggest memory hog) is restarted when node memory reaches 6.75G, an observed tipping point when running with 8GB max memory.

### Observability
Observability is handled by a combination of AWS CloudWatch and beaconcha.in monitoring.  

This allows for the creation of CloudWatch dashboards and use of the beaconcha.in mobile app to track the validator status and get application-level metrics and notifications (i.e. missed attestations).  I got most of what I needed from the free version of beaconcha.in's mobile app, but sprung for the `Plankton` license to see historical validator metrics and customize my notifications - highly recommended.

CloudWatch infrastructure alerts (high/low cpu, high memory) can be customized with thresholds, email forwarding and custom actions can be attached to respond to operational issues in an automated fashion.

### Automation
#### RestartOnHighMem
Due to geth's problems managing memory I added an EventRule that sends a command to restart geth when memory reaches the configured alerting threshold.

#### SNS Targets
Additional apps can be built to respond to alert notifications via SNS.

## Configuration
### CDK Environment
CDK will use your default AWS profile to extract the AWS account and region to deploy to, but this can be overridden via env vars:

```bash
export CDK_DEPLOY_PROFILE_FLAG="--profile everythingbiigadmin"
# Optionally, you can override the account and region for CDK to use here
export CDK_DEPLOY_ACCOUNT="213126473922"
export CDK_DEPLOY_REGION="us-east-2"
```

### Customizations
Most app configuration options are externalized and overridable via cdk.json or by specifying the named context flag via the `CDK_DEPLOY_EXTRA_CONTEXT` flag, for example:
```bash
export CDK_DEPLOY_EXTRA_CONTEXT="-c everythingbiig/ethereum-beacon-chain-aws:amiName=${AMI_NAME}"
bin/deploy-stacks.sh
```
Review `cdk.json` for all `everythingbiig/ethereum-beacon-chain-aws:*` to see what you can customize.  Most options are self-explanatory.

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

Look at `bin/deploy-stacks.sh` and other commands for configuration options like `CDK_DEPLOY_EXTRA_CONTEXT` that allow you to customize your deployment.

After deploying, scale up the ASGs.  You can run 
```bash
export ASG_NAME=<your asg name> 
export ASG_DESIRED_CAPACITY=1 
bin/set-desired-capacity.sh
```
## Todo
- Add healthcheck/watchdog to replace "bad" instances - in progress
- Automate validator import.
- Allow explicitly specifying availability zone for deployment