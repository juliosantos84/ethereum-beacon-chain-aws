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
import software.amazon.awscdk.services.ec2.IPeer;
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
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;



public class Goeth extends Stack {

    public static final IMachineImage GOETH_AMI         = MachineImage.lookup(
        LookupMachineImageProps.builder().name("goeth-20210605205208").build());
    static final Integer    GOETH_PORT                  = Integer.valueOf(30303);
    static final Integer    LIGHTHOUSE_PORT             = Integer.valueOf(9000);
    static final Integer    GRAFANA_PORT                = Integer.valueOf(3000);
    static final Integer    SSH_PORT                    = Integer.valueOf(22);

    static final String     VPC_CIDR                    = "10.1.0.0/16";
    static final Integer    MIN_GETH_INSTANCES          = Integer.valueOf(0);
    static final Integer    MAX_GETH_INSTANCES          = Integer.valueOf(1);

    static final IPeer      VPC_CIDR_PEER               = Peer.ipv4(VPC_CIDR);
    static final Size       ETH_DATA_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(1000));
    static final Duration   TARGET_DEREGISTRATION_DELAY = Duration.seconds(15);

    private Vpc                 vpc                             = null;
    private NetworkLoadBalancer publicLb                        = null;
    private SecurityGroup       backendAsgSecurityGroup         = null;
    private AutoScalingGroup    ethBackendAutoScalingGroup      = null;
    private List<IVolume>       chaindataVolumes        = null;

    public Goeth(final Construct scope, final String id, Vpc targetVpc) {
        this(scope, id, null, targetVpc);
    }

    public Goeth(final Construct scope, final String id, final StackProps props, Vpc targetVpc) {
        super(scope, id, props);

        // Configure a VPC
        this.vpc = targetVpc;
        
        // Configure a persistent volume for chain data
        getChaindataVolumes();

        // Autoscaling group for ETH backend
        getEthBackendAutoScalingGroup();
        
        // Configure a load balancer and ec2 ASG
        getPublicLoadBalancer();
    }

    protected List<IVolume> getChaindataVolumes() {
        if (this.chaindataVolumes == null) {
            this.chaindataVolumes = new ArrayList<IVolume>();
            for(String az : this.vpc.getAvailabilityZones()) {
                IVolume vol = Volume.Builder.create(this, "chaindataVolume"+az)
                .volumeName("chaindataVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(ETH_DATA_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                .removalPolicy(RemovalPolicy.DESTROY)
                .availabilityZone(az)
                .build();
                Tags.of(vol).add("Name", "chaindata");
                this.chaindataVolumes.add(vol);
            }
        }
        return this.chaindataVolumes;
         
    }

    protected NetworkLoadBalancer getPublicLoadBalancer() {
        if (this.publicLb == null) {
            this.publicLb = NetworkLoadBalancer.Builder.create(this, "publicLb")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                    .subnetType(SubnetType.PUBLIC).build())
                .internetFacing(Boolean.TRUE)
                .crossZoneEnabled(Boolean.TRUE)
                .build();
            
            NetworkListener goEthListener = this.publicLb.addListener("goeth", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP_UDP)
                    .port(GOETH_PORT)
                    .loadBalancer(this.publicLb)
                    .build());            

            NetworkListener sshListener = this.publicLb.addListener("ssh", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP)
                    .port(SSH_PORT)
                    .loadBalancer(this.publicLb)
                    .build());    

            NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("goeth", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getEthBackendAutoScalingGroup()))
                    .port(GOETH_PORT)
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );

            NetworkTargetGroup sshTargetGroup = sshListener.addTargets("ssh", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getEthBackendAutoScalingGroup()))
                    .port(Integer.valueOf(22))
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );
        }
        return this.publicLb;
    }

    protected SecurityGroup getEthBackendAsgSecurityGroup() {
        if (this.backendAsgSecurityGroup == null) {
            this.backendAsgSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
                .vpc(this.vpc)
                .build();

            backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(GOETH_PORT));
            backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.udp(GOETH_PORT));
            backendAsgSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22)); // TEST
            backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(22)); // TEST
        }
        return this.backendAsgSecurityGroup;
    }

    protected AutoScalingGroup getEthBackendAutoScalingGroup() {
        if (this.ethBackendAutoScalingGroup == null) {
            this.ethBackendAutoScalingGroup = AutoScalingGroup.Builder.create(this, "goeth")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.SMALL))
                .machineImage(GOETH_AMI)
                .keyName("eth-stack")
                .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
                .init(getGoethNodeCloudInit())
                .minCapacity(MIN_GETH_INSTANCES)
                .maxCapacity(MAX_GETH_INSTANCES)
                .allowAllOutbound(Boolean.TRUE)
                .securityGroup(this.getEthBackendAsgSecurityGroup())
                .updatePolicy(UpdatePolicy.rollingUpdate(
                    RollingUpdateOptions.builder()
                        .minInstancesInService(MIN_GETH_INSTANCES)
                        .build()))
                .signals(Signals.waitForMinCapacity(
                        SignalsOptions.builder().timeout(
                            Duration.minutes(Integer.valueOf(5))).build()))
                .build();
            this.ethBackendAutoScalingGroup.getGrantPrincipal()
                .addToPrincipalPolicy(PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ec2:DescribeVolumes"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList("*"))
                    .build());
        }

        // Grant the backendAsg access to attacht he volume
        for(IVolume vol : this.getChaindataVolumes()) {
            vol.grantAttachVolumeByResourceTag(
                this.ethBackendAutoScalingGroup.getGrantPrincipal(), 
                Arrays.asList(this.ethBackendAutoScalingGroup));
            vol.grantDetachVolumeByResourceTag(
                this.ethBackendAutoScalingGroup.getGrantPrincipal(), 
                Arrays.asList(this.ethBackendAutoScalingGroup));
        }

        return this.ethBackendAutoScalingGroup;
    }

    protected CloudFormationInit getGoethNodeCloudInit() {
        return CloudFormationInit.fromElements(
            InitCommand.shellCommand("sudo apt install awscli jq -y"),
            InitCommand.shellCommand("sudo systemctl daemon-reload"),
            // It's possible this command generates an error if the volume is not available
            // That's OK because the service is configured to retry every 30 seconds
            InitCommand.shellCommand("sudo systemctl start geth", 
                InitCommandOptions.builder()
                    .ignoreErrors(Boolean.TRUE).build()));
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }
}
