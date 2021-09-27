package com.everythingbiig.ethereum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
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
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Volume;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

public class Lighthouse extends Stack {
    public static final IMachineImage LIGHTHOUSE_AMI = MachineImage.lookup(
        LookupMachineImageProps.builder()
            .name("lighthouse-20210926181720").build());
    private AutoScalingGroup lighthouseAsg = null;
    private SecurityGroup lighthouseSecurityGroup = null;
    private List<IVolume> lighthouseVolumes        = null;
    private NetworkLoadBalancer privateLoadBalancer = null;
    private EthereumStackProps lighthouseProps = null;
    
    static final Integer LIGHTHOUSE_PORT = Integer.valueOf(9000);

    static final Size       LIGHTHOUSE_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(200));

    public Lighthouse(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public Lighthouse(final Construct scope, final String id, EthereumStackProps lighthouseProps, StackProps props) {
        super(scope, id, props);
        this.lighthouseProps = lighthouseProps;
        
        getLighthouseAsg();

        getPrivateLoadBalancer();
    }

    protected AutoScalingGroup getLighthouseAsg() {
        if (this.lighthouseAsg == null && lighthouseProps.getAppVpc() != null) {
            this.lighthouseAsg = AutoScalingGroup.Builder.create(this, "lighthouseAsg")
                .vpc(this.lighthouseProps.getAppVpc())
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MEDIUM))
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

            this.lighthouseAsg.getGrantPrincipal()
                .addToPrincipalPolicy(PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ec2:DescribeVolumes"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList("*"))
                    .build());

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
    
    protected NetworkLoadBalancer getPrivateLoadBalancer() {
        if (this.privateLoadBalancer == null) {
            this.privateLoadBalancer = NetworkLoadBalancer.Builder.create(this, "lighthouseLoadBalancer")
                .vpc(this.lighthouseProps.getAppVpc())
                .vpcSubnets(SubnetSelection.builder()
                    .subnetType(SubnetType.PRIVATE).build())
                .internetFacing(Boolean.FALSE)
                .crossZoneEnabled(Boolean.TRUE)
                .build();
            
            if(this.lighthouseProps.getPrivateHostedZone() != null) {
                ARecord.Builder.create(this, "lighthousePrivateARecord")
                    .zone(this.lighthouseProps.getPrivateHostedZone())
                    .target(RecordTarget.fromAlias(new LoadBalancerTarget(this.privateLoadBalancer)))
                    .recordName("lighthouse.private.ethereum.everythingbiig.com")
                    .ttl(Duration.minutes(1))
                    .build();
            }

            NetworkListener goEthListener = this.privateLoadBalancer.addListener("lighthouse", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP_UDP)
                    .port(LIGHTHOUSE_PORT)
                    .loadBalancer(this.privateLoadBalancer)
                    .build());            

            NetworkListener sshListener = this.privateLoadBalancer.addListener("ssh", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP)
                    .port(this.lighthouseProps.getAdministrationPort())
                    .loadBalancer(this.privateLoadBalancer)
                    .build());    

            NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("lighthouse", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getLighthouseAsg()))
                    .port(LIGHTHOUSE_PORT)
                    .deregistrationDelay(this.lighthouseProps.getTargetRegistrationDelay())
                    .build()
            );

            NetworkTargetGroup sshTargetGroup = sshListener.addTargets("ssh", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getLighthouseAsg()))
                    .port(Integer.valueOf(22))
                    .deregistrationDelay(this.lighthouseProps.getTargetRegistrationDelay())
                    .build()
            );
        }
        return this.privateLoadBalancer;
    }

    protected List<IVolume> getLighthouseVolumes() {
        if (this.lighthouseVolumes == null && this.lighthouseProps.getAppVpc() != null) {
            this.lighthouseVolumes = new ArrayList<IVolume>();
            for(String az : this.lighthouseProps.getAppVpc().getAvailabilityZones()) {
                IVolume vol = Volume.Builder.create(this, "lighthouseVolume"+az)
                .volumeName("lighthouseVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(LIGHTHOUSE_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                // .removalPolicy(RemovalPolicy.SNAPSHOT)
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
            InitCommand.shellCommand("sudo apt update"),
            InitCommand.shellCommand("sudo apt install awscli jq -y"),
            InitCommand.shellCommand("echo lighthouse > /home/ubuntu/volume-name-tag"),
            InitCommand.shellCommand("echo /var/lib/lighthouse > /home/ubuntu/volume-mount-path"),
            InitCommand.shellCommand("echo lighthousebeacon > /home/ubuntu/volume-mount-path-owner"),
            InitCommand.shellCommand("sudo systemctl daemon-reload"),
            InitCommand.shellCommand("sudo systemctl start lighthouse", 
                InitCommandOptions.builder()
                    .ignoreErrors(Boolean.TRUE).build())
            );
    }

    protected SecurityGroup getLighthouseSecurityGroup() {
        if (this.lighthouseSecurityGroup == null && this.lighthouseProps.getAppVpc() != null) {
            this.lighthouseSecurityGroup = SecurityGroup.Builder.create(this, "lighthouseSecurityGroup")
                .vpc(this.lighthouseProps.getAppVpc())
                .build();
            this.lighthouseSecurityGroup.addIngressRule(
                Peer.ipv4(this.lighthouseProps.getAppVpc().getVpcCidrBlock()), Port.tcp(LIGHTHOUSE_PORT));
            this.lighthouseSecurityGroup.addIngressRule(
                Peer.ipv4(this.lighthouseProps.getAppVpc().getVpcCidrBlock()), Port.udp(LIGHTHOUSE_PORT));
            this.lighthouseSecurityGroup.addIngressRule(
                this.lighthouseProps.getAdministrationCidr(), Port.tcp(22));
        }
        return this.lighthouseSecurityGroup;
    }
}


