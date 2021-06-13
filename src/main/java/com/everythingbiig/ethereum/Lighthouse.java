package com.everythingbiig.ethereum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Size;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.autoscaling.ApplyCloudFormationInitOptions;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.RollingUpdateOptions;
import software.amazon.awscdk.services.autoscaling.Signals;
import software.amazon.awscdk.services.autoscaling.SignalsOptions;
import software.amazon.awscdk.services.autoscaling.UpdatePolicy;
import software.amazon.awscdk.services.ec2.CloudFormationInit;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.IVolume;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InitCommandOptions;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.LookupMachineImageProps;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Volume;
import software.amazon.awscdk.services.ec2.Vpc;

public class Lighthouse extends Stack {
    public static final IMachineImage LIGHTHOUSE_AMI = MachineImage.lookup(
        LookupMachineImageProps.builder()
            .name("lighthouse-20210613175458").build());
    private Vpc vpc = null;
    private AutoScalingGroup lighthouseAsg = null;
    private SecurityGroup lighthouseSecurityGroup = null;
    private List<IVolume> lighthouseVolumes        = null;
    static final Size       LIGHTHOUSE_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(1000));

    public Lighthouse(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public Lighthouse(final Construct scope, final String id, EthereumStackProps lighthouseProps, StackProps props) {
        super(scope, id, props);
        if(lighthouseProps != null) {
            this.vpc = lighthouseProps.getAppVpc();
        }
        
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

            // // Grant the backendAsg access to attacht he volume
            for(IVolume vol : this.getLighthouseVolumes()) {
                vol.grantAttachVolumeByResourceTag(
                    this.lighthouseAsg.getGrantPrincipal(), 
                    Arrays.asList(this.lighthouseAsg));
                vol.grantDetachVolumeByResourceTag(
                    this.lighthouseAsg.getGrantPrincipal(), 
                    Arrays.asList(this.lighthouseAsg));
            }
        }

        return this.lighthouseAsg;
    }

    protected List<IVolume> getLighthouseVolumes() {
        if (this.lighthouseVolumes == null && this.vpc != null) {
            this.lighthouseVolumes = new ArrayList<IVolume>();
            for(String az : this.vpc.getAvailabilityZones()) {
                IVolume vol = Volume.Builder.create(this, "lighthouseVolume"+az)
                .volumeName("lighthouseVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(LIGHTHOUSE_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                .removalPolicy(RemovalPolicy.RETAIN)
                .availabilityZone(az)
                .build();
                Tags.of(vol).add("Name", "lighthouse");
                this.lighthouseVolumes.add(vol);
            }
        }
        return this.lighthouseVolumes;
         
    }

    private CloudFormationInit getLighthouseCloudInit() {
        return CloudFormationInit.fromElements(
                InitCommand.shellCommand("echo lighthouse > /home/ubuntu/volume-name-tag"),
                InitCommand.shellCommand("echo /var/lib/lighthouse > /home/ubuntu/volume-mount-path"),
                InitCommand.shellCommand("echo lighthousebeacon > /home/ubuntu/volume-mount-path-owner"),
                InitCommand.shellCommand("sudo systemctl start lighthouse", 
                    InitCommandOptions.builder()
                        .ignoreErrors(Boolean.TRUE).build())
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


