package com.everythingbiig.ethereum;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

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
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.IVolume;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InitCommandOptions;
import software.amazon.awscdk.services.ec2.InitElement;
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
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;



public class EthereumBeaconChainNode extends Stack {
    static final Integer    GOETH_PORT                  = Integer.valueOf(30303);
    static final Integer    GOETH_RPC_PORT                  = Integer.valueOf(8545);
    static final Integer    GRAFANA_PORT                = Integer.valueOf(3000);
    static final Integer    SSH_PORT                    = Integer.valueOf(22);

    static final String     VPC_CIDR                    = "10.1.0.0/16";
    static final Integer    MIN_GETH_INSTANCES          = Integer.valueOf(0);
    static final Integer    MAX_GETH_INSTANCES          = Integer.valueOf(1);

    static final IPeer      VPC_CIDR_PEER               = Peer.ipv4(VPC_CIDR);
    static final Size       ETH_DATA_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(150));
    static final Duration   TARGET_DEREGISTRATION_DELAY = Duration.seconds(15);

    private NetworkLoadBalancer privateLoadBalancer                        = null;
    private SecurityGroup       autoscalingGroupSecurityGroup         = null;
    private AutoScalingGroup    autoscalingGroup      = null;
    private List<IVolume>       volumes        = null;
    private EthereumBeaconChainProps ethBeaconChainProps = null;

    public EthereumBeaconChainNode(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public EthereumBeaconChainNode(final Construct scope, final String id, final EthereumBeaconChainProps goethProps, final StackProps props) {
        super(scope, id, props);

        this.ethBeaconChainProps = goethProps;

        // Configure a persistent volume for chain data
        createVolumes();

        // Autoscaling group for ETH backend
        createAutoscalingGroup();

        if (enableSessionManager()) {
            allowSessionManagerAccess();
        }

        // Allow the ASG instances to describe and attach to volumes
        allowVolumeAttachmentToAsg();
        
        // Configure a load balancer and ec2 ASG
        createPrivateLoadBalancer();
    }

    private void allowSessionManagerAccess() {
        // Allow session manager connections
        // TODO does this need to be more restrictive?
        this.getAutoscalingGroup().getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ec2-instance-connect:SendSSHPublicKey"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                    String.format("arn:aws:ec2:%s:%s:instance/*", getRegion(), getAccount())))
                .build());
        this.getAutoscalingGroup().getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ssm:StartSession"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                    String.format("arn:aws:ec2:%s:%s:instance/*", getRegion(), getAccount())))
                .build());
    }

    private void allowVolumeAttachmentToAsg() {
        if(this.autoscalingGroup != null) {
            this.autoscalingGroup.getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ec2:DescribeVolumes"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList("*"))
                .build());
        }
        if(this.volumes != null) {
            for(IVolume vol : this.createVolumes()) {
                vol.grantAttachVolumeByResourceTag(
                    this.autoscalingGroup.getGrantPrincipal(), 
                    Arrays.asList(this.autoscalingGroup));
                vol.grantDetachVolumeByResourceTag(
                    this.autoscalingGroup.getGrantPrincipal(), 
                    Arrays.asList(this.autoscalingGroup));
            }
        }
    }

    protected List<String> getSinleAvailabilityZone(){
        return getAvailabilityZones().subList(0, 1);
    }

    protected List<IVolume> createVolumes() {
        if (this.volumes == null && this.ethBeaconChainProps.getAppVpc() != null) {
            
            this.volumes = new ArrayList<IVolume>();
            for(String az : getSinleAvailabilityZone()) {
                IVolume vol = Volume.Builder.create(this, "chaindataVolume"+az)
                .volumeName("chaindataVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(ETH_DATA_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                // .removalPolicy(RemovalPolicy.SNAPSHOT)
                .availabilityZone(az)
                .build();
                Tags.of(vol).add("Name", "goeth");
                this.volumes.add(vol);
            }
        }
        return this.volumes;
         
    }

    protected NetworkLoadBalancer createPrivateLoadBalancer() {
        this.privateLoadBalancer = NetworkLoadBalancer.Builder.create(this, "goethLoadBalancer")
            .vpc(this.ethBeaconChainProps.getAppVpc())
            .vpcSubnets(SubnetSelection.builder()
                .subnetType(SubnetType.PRIVATE).build())
            .internetFacing(Boolean.FALSE)
            .crossZoneEnabled(Boolean.TRUE)
            .build();
        
        if(this.ethBeaconChainProps.getPrivateHostedZone() != null) {
            ARecord.Builder.create(this, "goethPrivateARecord")
                .zone(this.ethBeaconChainProps.getPrivateHostedZone())
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(this.privateLoadBalancer)))
                .recordName(EthereumBeaconChainNode.this.getRecordName())
                .build();
        }

        addListenerAndTarget("goeth", Protocol.TCP_UDP, GOETH_PORT, null);
        addListenerAndTarget("goethRpc", Protocol.TCP, GOETH_RPC_PORT, null);
        addListenerAndTarget("lighthouse", Protocol.TCP, Integer.valueOf(9000), null);

        return this.privateLoadBalancer;
    }

    private HealthCheck createHealthCheck(String path, Protocol protocol, String port) {
        return HealthCheck.builder()
            .enabled(Boolean.TRUE)
            .healthyHttpCodes("200-299")
            .healthyThresholdCount(2)
            .unhealthyThresholdCount(2)
            .interval(Duration.seconds(30))
            // .timeout(Duration.seconds(5)) // Not supported for NLB
            .path(path)
            .protocol(protocol)
            .port(port)
            .build();
    }

    private void addListenerAndTarget(String id, Protocol protocol, Integer port, HealthCheck healthCheck) {
        AddNetworkTargetsProps.Builder targetPropsBuilder = AddNetworkTargetsProps.builder()
            .targets(Arrays.asList(this.getAutoscalingGroup()))
            .port(port)
            //TODO .healthcheck
            .deregistrationDelay(TARGET_DEREGISTRATION_DELAY);
        if (healthCheck != null) {
            targetPropsBuilder.healthCheck(healthCheck);
        }
        this.privateLoadBalancer.addListener(id, 
            NetworkListenerProps.builder()
                .protocol(protocol)
                .port(port)
                .loadBalancer(this.privateLoadBalancer)
                .build()
        ).addTargets(id, targetPropsBuilder.build());
        
        if (Protocol.TCP == protocol || Protocol.TCP_UDP == protocol) {
            getAutoscalingGroupSecurityGroup().addIngressRule(
                Peer.ipv4(this.ethBeaconChainProps.getAppVpc().getVpcCidrBlock()), Port.tcp(port));
        }
        if (Protocol.UDP == protocol || Protocol.TCP_UDP == protocol) {
            getAutoscalingGroupSecurityGroup().addIngressRule(
                Peer.ipv4(this.ethBeaconChainProps.getAppVpc().getVpcCidrBlock()), Port.udp(port));
        }
    }

    /**
     * beaconchain.<region>.<testnet|mainnet>.<private-hosted-zone>
     * @return
     */
    private String getRecordName() {
        return String.format("%s.%s", "goeth", 
            (String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:privateHostedZone"));
    }

    protected AutoScalingGroup getAutoscalingGroup() {
        return this.autoscalingGroup;
    }

    protected SecurityGroup getAutoscalingGroupSecurityGroup() {
        if (this.autoscalingGroupSecurityGroup == null && this.ethBeaconChainProps.getAppVpc() != null) {
            this.autoscalingGroupSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
                .vpc(this.ethBeaconChainProps.getAppVpc())
                .build();
        }
        return this.autoscalingGroupSecurityGroup;
    }

    protected AutoScalingGroup createAutoscalingGroup() {
        if (this.ethBeaconChainProps.getAppVpc() != null) {
            this.autoscalingGroup = AutoScalingGroup.Builder.create(this, "goeth")
                .vpc(this.ethBeaconChainProps.getAppVpc())
                .vpcSubnets(
                    SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE)
                        .availabilityZones(getSinleAvailabilityZone())
                        .build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.SMALL))
                .machineImage(EthereumBeaconChainNode.this.getMachineImage())
                .keyName((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:keyPair"))
                .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
                .init(getCloudInit())
                .minCapacity(MIN_GETH_INSTANCES)
                .maxCapacity(MAX_GETH_INSTANCES)
                .allowAllOutbound(Boolean.TRUE)
                .securityGroup(this.getAutoscalingGroupSecurityGroup())
                .updatePolicy(UpdatePolicy.rollingUpdate(
                    RollingUpdateOptions.builder()
                        .minInstancesInService(MIN_GETH_INSTANCES)
                        .build()))
                .signals(Signals.waitForMinCapacity(
                        SignalsOptions.builder().timeout(
                            Duration.minutes(Integer.valueOf(5))).build()))
                .build();
                // Add CloudWatch policies
                this.autoscalingGroup.getRole().addManagedPolicy(
                    ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"));
                this.autoscalingGroup.getRole().addManagedPolicy(
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"));
        }

        return this.autoscalingGroup;
    }

    private IMachineImage getMachineImage() {
        return MachineImage.lookup(
            LookupMachineImageProps.builder()
                .name((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:amiName")).build());
    }

    protected Boolean enableSessionManager() {
        return (Boolean) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableSessionManager");
    }

    protected CloudFormationInit getCloudInit() {
        return CloudFormationInit.fromElements(
            // Enable the volume services
            createServiceToggleInitCommand("chaindata-volume-attachment", "enable --now"),
            createServiceToggleInitCommand("var-lib-chaindata.mount", "enable --now"),
            // Set environment vars
            createServiceConfigurationInitCommand("geth", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            createServiceConfigurationInitCommand("lighthousebeacon", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            createServiceConfigurationInitCommand("lighthousevalidator", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            // Start services
            createServiceToggleInitCommand("geth", "enable --now"),
            createServiceToggleInitCommand("lighthousebeacon", "enable --now"),
            createServiceToggleInitCommand("lighthousevalidator", enableValidator() ? "enable --now" : "disable"));
    }

    private Boolean enableValidator() {
        return (Boolean) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableValidator");
    }
    private @NotNull InitElement createServiceConfigurationInitCommand(String serviceName, String beaconChainEnvironment) {
        return InitCommand.shellCommand(
            MessageFormat.format("sudo ln -s /etc/systemd/system/{0}/{0}.service.{1}.env /etc/systemd/system/{0}.service.env", serviceName, beaconChainEnvironment),
            InitCommandOptions.builder().ignoreErrors(Boolean.TRUE).build()
        );
    }

    private InitCommand createServiceToggleInitCommand(String serviceName, String command) {
        return InitCommand.shellCommand(
            String.format("sudo systemctl %s %s", command, serviceName),
            InitCommandOptions.builder().ignoreErrors(Boolean.TRUE).build()
        );
    }
}