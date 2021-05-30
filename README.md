# eth2-beacon-chain
A project to learn how to operate the ETH2.0 beacon chain software.

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

## Architecture
I'm following [this]() guide to spin up a testnet stack on AWS:
- VPC for workload isolation
- A load balancer deployed to a public subnet to front the instances
- An ASG maintain the desired instance count (max=1)
- An EBS volume to persist chain data across hosts' lifecycle
- Scripted volume attachment/detachment to avoid loss of data

## Deployments
An ETH2 validator node has unique validator keys which can only be used by one validator at a time.  
If two instances of the stack are running with the same keys then it's possible both will generate different blocks and lead to financial penalties.

### Before deploying
- Ensure the current state is in source control or can otherwise be redeployed
- Ensure the changeset to be deployed is in source control
- Ensure the target environment has been boostrapped:
    - `cdk boostrap` must be run
    - `ethBackendKeyPair` must be created in the console
    
### Deploying
To ensure we're never running two nodes at a time we must:
1. Scale down the ASG to desired instance count = 0
    1. This forces the geth service to stop and the volume to be detached
2. Deploy the updated stack
3. Scale up the ASG to desired instance count = 1
    1. This forces the volume to be attached and the geth service to start

### After deploying
- Ensure the current state is in source control.

## Todo

Continue following [this](https://someresat.medium.com/guide-to-staking-on-ethereum-2-0-ubuntu-pyrmont-lighthouse-a634d3b87393) guide.

[X] - Create VPC network

[X] - Create ETH Instance

[X] - Join test net