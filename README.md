# ethereum-beacon-chain-aws
Code to operate ethereum beacon chain on AWS.  

This deployment is based on [this](https://someresat.medium.com/guide-to-staking-on-ethereum-2-0-ubuntu-pyrmont-lighthouse-a634d3b87393) guide for deploying to the Pyrmont testnet.

## Architecture

![Ethereum Beacon Chain Service](docs/Ethereum_Beacon_Chain_Service.png)

- VPCs for workload isolation
- A load balancer deployed to a public subnet to front the instances
- An ASG maintain the desired instance count (max=1)
- An EBS volume to persist chain data across hosts' lifecycle
- Scripted volume attachment/detachment to avoid loss of data

## Deployments
Set the env vars and run the deploy script:

```bash
export BASTION_ALLOWED_CIDR=1.1.1.1/32
bin/deploy-stacks.sh
```
or deploy a specific stack:

```bash
BASTION_ALLOWED_CIDR=1.1.1.1/32 cdk deploy ethereumBeaconChainService/administration --require-approval never
```

### After deploying
- Ensure the current state is in source control.

## Todo

[X] - Create VPC network

[X] - Create ETH Instance

[X] - Join goeth to goerli testnet

[X] - Join lighthouse to pyrmont testnet

[ ] - Add monitoring

[ ] - Automate validator wallet import
```bash
sudo lighthouse --network pyrmont account validator import --keystore /home/ubuntu/validator_keys/keystore.json --datadir /var/lib/lighthouse
```
[ ] - Externalize instance-specific settings (VPC CIDRs, Domain names, etc)

## Project layout

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation