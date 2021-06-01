package com.everythingbiig.ethereum;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Size;
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
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.IVolume;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InitCommandOptions;
import software.amazon.awscdk.services.ec2.InitFile;
import software.amazon.awscdk.services.ec2.InitFileAssetOptions;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Volume;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;



public class EthBeaconChainStack extends Stack {

    static final Integer    GOETH_PORT                  = Integer.valueOf(30303);
    static final Integer    LIGHTHOUSE_PORT             = Integer.valueOf(9000);
    static final Integer    GRAFANA_PORT                = Integer.valueOf(3000);
    static final Integer    SSH_PORT                    = Integer.valueOf(22);

    static final String     VPC_CIDR                    = "10.1.0.0/16";
    static final Integer    MIN_GETH_INSTANCES          = Integer.valueOf(0);
    static final Integer    MAX_GETH_INSTANCES          = Integer.valueOf(1);

    static final IPeer      VPC_CIDR_PEER               = Peer.ipv4(VPC_CIDR);
    static final Size       ETH_DATA_VOLUME_SIZE        = Size.gibibytes(Integer.valueOf(2000));
    static final Duration   TARGET_DEREGISTRATION_DELAY = Duration.seconds(15);

    private Vpc                 vpc                             = null;
    private NetworkLoadBalancer publicLb                        = null;
    private SecurityGroup       backendAsgSecurityGroup         = null;
    private UserData            userdata                        = null;
    private AutoScalingGroup    ethBackendAutoScalingGroup      = null;
    private IVolume             ethChainDataVolume              = null;

    public EthBeaconChainStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public EthBeaconChainStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Configure a VPC
        this.vpc = Vpc.Builder.create(this, "EthVpc")
            .cidr(VPC_CIDR)
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(1))
            .build();
        
        // Configure a persistent volume for chain data
        this.ethChainDataVolume = getEthChainDataVolume();

        // Autoscaling group for ETH backend
        this.ethBackendAutoScalingGroup = getEthBackendAutoScalingGroup();
        
        // Configure a load balancer and ec2 ASG
        this.publicLb = getPublicLoadBalancer();
    }

    protected IVolume getEthChainDataVolume() {
        if (this.ethChainDataVolume == null) {
            this.ethChainDataVolume = Volume.Builder.create(this, "ethVolume")
                .volumeName("ethVolume")
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(ETH_DATA_VOLUME_SIZE)
                .encrypted(Boolean.TRUE)
                .removalPolicy(RemovalPolicy.SNAPSHOT)
                .availabilityZone(this.vpc.getPrivateSubnets().get(0).getAvailabilityZone())
                .build();
        }
        return this.ethChainDataVolume;
         
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
            
            NetworkListener goEthListener = this.publicLb.addListener("goEth", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP_UDP)
                    .port(GOETH_PORT)
                    .loadBalancer(this.publicLb)
                    .build());            

            NetworkListener testListener = this.publicLb.addListener("ssh", 
                NetworkListenerProps.builder()
                    .protocol(Protocol.TCP)
                    .port(SSH_PORT)
                    .loadBalancer(this.publicLb)
                    .build());    

            NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("ethBackendTargets", 
                AddNetworkTargetsProps.builder()
                    .targets(Arrays.asList(this.getEthBackendAutoScalingGroup()))
                    .port(GOETH_PORT)
                    .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                    .build()
            );

            NetworkTargetGroup testTargetGroup = testListener.addTargets("testTargets", 
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
            this.ethBackendAutoScalingGroup = AutoScalingGroup.Builder.create(this, "backendAsg")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MEDIUM))
                .machineImage(getMachineImage())
                .keyName("eth-stack")
                .userData(getEthUserData())
                .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
                .init(getEth2NodeCloudInit())
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
        }

        this.getEthUserData().addSignalOnExitCommand(ethBackendAutoScalingGroup);

        // Grant the backendAsg access to attacht he volume
        this.getEthChainDataVolume().grantAttachVolumeByResourceTag(
            this.ethBackendAutoScalingGroup.getGrantPrincipal(), 
            Arrays.asList(this.ethBackendAutoScalingGroup));
        
        this.getEthChainDataVolume().grantDetachVolumeByResourceTag(
            this.ethBackendAutoScalingGroup.getGrantPrincipal(), 
            Arrays.asList(this.ethBackendAutoScalingGroup));

        return this.ethBackendAutoScalingGroup;
    }

    private static IMachineImage getMachineImage() {
        return MachineImage.genericLinux(
            new HashMap<String, String>(){
            {
                put("us-east-1", "ami-0db6c6238a40c0681");
                put("us-east-2", "ami-03b6c8bd55e00d5ed");
            }});
    }

    protected CloudFormationInit getEth2NodeCloudInit() {
        return CloudFormationInit.fromElements(
                InitCommand.shellCommand("sudo add-apt-repository -y ppa:ethereum/ethereum"),
                InitCommand.shellCommand("sudo apt update"),
                InitCommand.shellCommand("sudo apt install awscli -y"),
                InitCommand.shellCommand("sudo apt install geth"),
                // Store the volume data
                InitCommand.shellCommand("echo ${ETH_VOLUME_ID} > /home/ubuntu/eth-volume-id && echo ${ETH_VOLUME_REGION} > /home/ubuntu/eth-volume-region", 
                    InitCommandOptions.builder().env(
                        new HashMap<String, String>(){
                            {
                                put("ETH_VOLUME_ID", EthBeaconChainStack.this.getEthChainDataVolume().getVolumeId());
                                put("ETH_VOLUME_REGION", EthBeaconChainStack.this.getRegion());
                            }
                        }).build()),
                InitFile.fromAsset("/etc/systemd/system/geth.service", "src/main/resources/units/geth.service"),
                InitFile.fromAsset("/usr/local/bin/attach-goeth-volume.sh", "src/main/resources/bin/attach-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("goeth")
                        .group("goeth")
                        .mode("755")
                        .build()),
                InitFile.fromAsset("/usr/local/bin/detach-goeth-volume.sh", "src/main/resources/bin/detach-goeth-volume.sh", 
                        InitFileAssetOptions.builder()
                            .owner("goeth")
                            .group("goeth")
                            .mode("755")
                            .build()),
                InitFile.fromAsset("/usr/local/bin/format-goeth-volume.sh", "src/main/resources/bin/format-goeth-volume.sh", 
                        InitFileAssetOptions.builder()
                            .owner("goeth")
                            .group("goeth")
                            .mode("755")
                            .build()),
                InitFile.fromAsset("/usr/local/bin/mount-goeth-volume.sh", "src/main/resources/bin/mount-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("goeth")
                        .group("goeth")
                        .mode("755")
                        .build()),
                InitFile.fromAsset("/usr/local/bin/unmount-goeth-volume.sh", "src/main/resources/bin/unmount-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("goeth")
                        .group("goeth")
                        .mode("755")
                        .build()),
                InitCommand.shellCommand("sudo systemctl daemon-reload"),
                // It's possible this command generates an error if the volume is not available
                // That's OK because the service is configured to retry every 30 seconds
                InitCommand.shellCommand("sudo systemctl start geth", 
                    InitCommandOptions.builder()
                        .ignoreErrors(Boolean.TRUE).build()),
                InitCommand.shellCommand("sudo systemctl status geth")
                );
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }

    public UserData getEthUserData() {
        if (this.userdata == null) {
            this.userdata = UserData.forLinux();
            userdata.addCommands(
                "sudo useradd --no-create-home --shell /bin/false goeth",
                // Install cfn helper scripts
                "sudo mkdir -p /opt/aws",
                "sudo chown -R ubuntu:users /opt/aws",
                "curl https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-py3-latest.tar.gz --output /tmp/aws-cfn-bootstrap-py3-latest.tar.gz",
                "tar -xvf /tmp/aws-cfn-bootstrap-py3-latest.tar.gz -C /tmp/",
                "mv /tmp/aws-cfn-bootstrap-2.0/* /opt/aws",
                "cd /opt/aws",
                "sudo python3 setup.py install --prefix /opt/aws --install-lib /usr/lib/python3.8",
                "chmod +x /opt/aws/bin/*",
                "sudo ln -s /opt/aws/bin/cfn-hup /etc/init.d/cfn-hup");
        }
        return this.userdata;
    }
}
