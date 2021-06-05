package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.ApplyCloudFormationInitOptions;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.RollingUpdateOptions;
import software.amazon.awscdk.services.autoscaling.Signals;
import software.amazon.awscdk.services.autoscaling.SignalsOptions;
import software.amazon.awscdk.services.autoscaling.UpdatePolicy;
import software.amazon.awscdk.services.ec2.CloudFormationInit;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.LookupMachineImageProps;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

public class Lighthouse extends Stack {
    public static final IMachineImage LIGHTHOUSE_AMI = MachineImage.lookup(
        LookupMachineImageProps.builder()
            .name("lighthouse-20210605180126").build());
    private Vpc vpc = null;
    private AutoScalingGroup lighthouseAsg = null;
    private SecurityGroup lighthouseSecurityGroup = null;
    
    public Lighthouse(final Construct scope, final String id, Vpc targetVpc) {
        this(scope, id, null, targetVpc);
    }

    public Lighthouse(final Construct scope, final String id, StackProps props, Vpc targetVpc) {
        super(scope, id, props);
        this.vpc = targetVpc;
        
        getLighthouseAsg();
    }

    protected AutoScalingGroup getLighthouseAsg() {
        if (this.lighthouseAsg == null) {
            this.lighthouseAsg = AutoScalingGroup.Builder.create(this, "lighthouseAsg")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.SMALL))
                .machineImage(LIGHTHOUSE_AMI)
                .keyName("eth-stack")
                .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
                .init(this.getLighthouseCloudInit())
                .minCapacity(0)
                .maxCapacity(1)
                .allowAllOutbound(Boolean.TRUE)
                .securityGroup(this.getLighthouseSecurityGroup())
                .updatePolicy(UpdatePolicy.rollingUpdate(
                    RollingUpdateOptions.builder()
                        .minInstancesInService(0)
                        .build()))
                .signals(Signals.waitForMinCapacity(
                        SignalsOptions.builder().timeout(
                            Duration.minutes(Integer.valueOf(5))).build()))
                .build();
        }

        // this.getEthUserData().addSignalOnExitCommand(lighthouseAsg);

        // // Grant the backendAsg access to attacht he volume
        // this.getEthChainDataVolume().grantAttachVolumeByResourceTag(
        //     this.lighthouseAsg.getGrantPrincipal(), 
        //     Arrays.asList(this.lighthouseAsg));
        
        // this.getEthChainDataVolume().grantDetachVolumeByResourceTag(
        //     this.lighthouseAsg.getGrantPrincipal(), 
        //     Arrays.asList(this.lighthouseAsg));

        return this.lighthouseAsg;
    }

    private CloudFormationInit getLighthouseCloudInit() {
        return CloudFormationInit.fromElements(
                InitCommand.shellCommand("echo initted")                
                );
    }

    protected SecurityGroup getLighthouseSecurityGroup() {
        if (this.lighthouseSecurityGroup == null) {
            this.lighthouseSecurityGroup = SecurityGroup.Builder.create(this, "lighthouseSecurityGroup")
                .vpc(this.vpc)
                .build();

            // lighthouseSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(GOETH_PORT));
            // lighthouseSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22)); // TEST
        }
        return this.lighthouseSecurityGroup;
    }
}


