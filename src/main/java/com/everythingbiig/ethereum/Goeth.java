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
import software.amazon.awscdk.services.ec2.VpcEndpointService;
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



public class Goeth extends Stack {

    public static final IMachineImage GOETH_AMI         = MachineImage.lookup(
        LookupMachineImageProps.builder().name("goeth-20210614012218").build());
    static final Integer    GOETH_PORT                  = Integer.valueOf(30303);
    static final Integer    GOETH_RPC_PORT                  = Integer.valueOf(8545);
    static final Integer    GRAFANA_PORT                = Integer.valueOf(3000);
    static final Integer    SSH_PORT                    = Integer.valueOf(22);

    static final String     VPC_CIDR                    = "10.1.0.0/16";
    static final Integer    MIN_GETH_INSTANCES          = Integer.valueOf(0);
    static final Integer    MAX_GETH_INSTANCES          = Integer.valueOf(1);

    static final IPeer      VPC_CIDR_PEER               = Peer.ipv4(VPC_CIDR);
    static final Size       ETH_DATA_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(1000));
    static final Duration   TARGET_DEREGISTRATION_DELAY = Duration.seconds(15);


    private NetworkLoadBalancer privateLoadBalancer                        = null;
    private SecurityGroup       backendAsgSecurityGroup         = null;
    private AutoScalingGroup    goethAsg      = null;
    private List<IVolume>       chaindataVolumes        = null;
    private VpcEndpointService privateLoadBalancerVpcEndpoint = null;
    private EthereumStackProps goethProps = null;

    public Goeth(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public Goeth(final Construct scope, final String id, final EthereumStackProps goethProps, final StackProps props) {
        super(scope, id, props);

        this.goethProps = goethProps;  

        // Configure a persistent volume for chain data
        getChaindataVolumes();

        // Autoscaling group for ETH backend
        getGoethBackendAsg();
        
        // Configure a load balancer and ec2 ASG
        getPrivateLoadBalancer();
    }

    protected List<IVolume> getChaindataVolumes() {
        if (this.chaindataVolumes == null && this.goethProps.getAppVpc() != null) {
            
            this.chaindataVolumes = new ArrayList<IVolume>();
            for(String az : this.goethProps.getAppVpc().getAvailabilityZones()) {
                IVolume vol = Volume.Builder.create(this, "chaindataVolume"+az)
                .volumeName("chaindataVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(ETH_DATA_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                .removalPolicy(RemovalPolicy.SNAPSHOT)
                .availabilityZone(az)
                .build();
                Tags.of(vol).add("Name", "goeth");
                this.chaindataVolumes.add(vol);
            }
        }
        return this.chaindataVolumes;
         
    }

    protected NetworkLoadBalancer getPrivateLoadBalancer() {
        if (this.privateLoadBalancer == null) {
            this.privateLoadBalancer = NetworkLoadBalancer.Builder.create(this, "goethLoadBalancer")
                .vpc(this.goethProps.getAppVpc())
                .vpcSubnets(SubnetSelection.builder()
                    .subnetType(SubnetType.PRIVATE).build())
                .internetFacing(Boolean.FALSE)
                .crossZoneEnabled(Boolean.TRUE)
                .build();
            
            if(this.goethProps.getPrivateHostedZone() != null) {
                ARecord.Builder.create(this, "goethPrivateARecord")
                    .zone(this.goethProps.getPrivateHostedZone())
                    .target(RecordTarget.fromAlias(new LoadBalancerTarget(this.privateLoadBalancer)))
                    .recordName("goeth.private.ethereum.everythingbiig.com")
                    .build();
            }

            NetworkListener goEthListener = this.privateLoadBalancer.addListener("goeth", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP_UDP)
                    .port(GOETH_PORT)
                    .loadBalancer(this.privateLoadBalancer)
                    .build());

            NetworkListener goethRpcListener = this.privateLoadBalancer.addListener("goethRpc", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP)
                    .port(GOETH_RPC_PORT)
                    .loadBalancer(this.privateLoadBalancer)
                    .build());          

            NetworkListener sshListener = this.privateLoadBalancer.addListener("ssh", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP)
                    .port(SSH_PORT)
                    .loadBalancer(this.privateLoadBalancer)
                    .build());    

            NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("goeth", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getGoethBackendAsg()))
                    .port(GOETH_PORT)
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );

            NetworkTargetGroup goethRpcTargetGroup = goethRpcListener.addTargets("goethRpc", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getGoethBackendAsg()))
                    .port(GOETH_RPC_PORT)
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );

            NetworkTargetGroup sshTargetGroup = sshListener.addTargets("ssh", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getGoethBackendAsg()))
                    .port(Integer.valueOf(22))
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );
        }
        return this.privateLoadBalancer;
    }

    protected SecurityGroup getEthBackendAsgSecurityGroup() {
        if (this.backendAsgSecurityGroup == null && this.goethProps.getAppVpc() != null) {
            this.backendAsgSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
                .vpc(this.goethProps.getAppVpc())
                .build();
            backendAsgSecurityGroup.addIngressRule(Peer.ipv4(this.goethProps.getAppVpc().getVpcCidrBlock()), Port.tcp(GOETH_PORT));
            backendAsgSecurityGroup.addIngressRule(Peer.ipv4(this.goethProps.getAppVpc().getVpcCidrBlock()), Port.tcp(GOETH_RPC_PORT));
            backendAsgSecurityGroup.addIngressRule(Peer.ipv4(this.goethProps.getAppVpc().getVpcCidrBlock()), Port.udp(GOETH_PORT));
            backendAsgSecurityGroup.addIngressRule(this.goethProps.getAdministrationCidr(), Port.tcp(22));
        }
        return this.backendAsgSecurityGroup;
    }

    protected AutoScalingGroup getGoethBackendAsg() {
        if (this.goethAsg == null && this.goethProps.getAppVpc() != null) {
            this.goethAsg = AutoScalingGroup.Builder.create(this, "goeth")
                .vpc(this.goethProps.getAppVpc())
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

            this.goethAsg.getGrantPrincipal()
                .addToPrincipalPolicy(PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ec2:DescribeVolumes"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList("*"))
                    .build());

            // Grant the backendAsg access to attacht he volume
            for(IVolume vol : this.getChaindataVolumes()) {
                vol.grantAttachVolumeByResourceTag(
                    this.goethAsg.getGrantPrincipal(), 
                    Arrays.asList(this.goethAsg));
                vol.grantDetachVolumeByResourceTag(
                    this.goethAsg.getGrantPrincipal(), 
                    Arrays.asList(this.goethAsg));
            }
        }

        return this.goethAsg;
    }

    protected CloudFormationInit getGoethNodeCloudInit() {
        return CloudFormationInit.fromElements(
            InitCommand.shellCommand("sudo apt update"),
            InitCommand.shellCommand("sudo apt install awscli jq -y"),
            InitCommand.shellCommand("echo goeth > /home/ubuntu/volume-name-tag"),
            InitCommand.shellCommand("echo /var/lib/goethereum > /home/ubuntu/volume-mount-path"),
            InitCommand.shellCommand("echo goeth > /home/ubuntu/volume-mount-path-owner"),
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
