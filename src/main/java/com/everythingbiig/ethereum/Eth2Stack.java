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



public class Eth2Stack extends Stack {

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

    private Vpc                 vpc                     = null;
    private NetworkLoadBalancer publicLb                = null;
    private SecurityGroup       backendAsgSecurityGroup = null;
    private UserData            userdata                = null;
    private AutoScalingGroup    backendAsg              = null;
    private IVolume             ethVolume               = null;

    public Eth2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Eth2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Configure a VPC
        this.vpc = Vpc.Builder.create(this, "EthVpc")
            .cidr(VPC_CIDR)
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(1))
            .build();
        
        // Configure a persistent volume for chain data
        initEthVolume();

        // Autoscaling group for ETH backend
        initBackendAsg();
        
        // Configure a load balancer and ec2 ASG
        initPublicLoadBalancer();
    }

    private void initEthVolume() {
        this.ethVolume = Volume.Builder.create(this, "ethVolume")
            .volumeName("ethVolume")
            .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
            .size(ETH_DATA_VOLUME_SIZE)
            .encrypted(Boolean.TRUE)
            .removalPolicy(RemovalPolicy.SNAPSHOT)
            .availabilityZone(this.vpc.getPrivateSubnets().get(0).getAvailabilityZone())
            .build();
    }

    private void initPublicLoadBalancer() {
        this.publicLb = NetworkLoadBalancer.Builder.create(this, "publicLb")
            .vpc(vpc)
            .vpcSubnets(SubnetSelection.builder()
                .subnetType(SubnetType.PUBLIC).build())
            .internetFacing(Boolean.TRUE)
            .crossZoneEnabled(Boolean.TRUE)
            .build();
        
        NetworkListener goEthListener = publicLb.addListener("goEth", 
            NetworkListenerProps.builder()
                .protocol(Protocol.TCP_UDP)
                .port(GOETH_PORT)
                .loadBalancer(publicLb)
                .build());            

        NetworkListener testListener = publicLb.addListener("ssh", 
            NetworkListenerProps.builder()
                .protocol(Protocol.TCP)
                .port(SSH_PORT)
                .loadBalancer(publicLb)
                .build());    

        NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("ethBackendTargets", 
            AddNetworkTargetsProps.builder()
                .targets(Arrays.asList(backendAsg))
                .port(GOETH_PORT)
                .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                    .healthyThresholdCount(Integer.valueOf(1))
                    .interval(Duration.seconds(15))
                    .build())
                .build()
        );

        NetworkTargetGroup testTargetGroup = testListener.addTargets("testTargets", 
            AddNetworkTargetsProps.builder()
                .targets(Arrays.asList(backendAsg))
                .port(Integer.valueOf(22))
                .deregistrationDelay(TARGET_DEREGISTRATION_DELAY)
                .build()
        );
    }

    private void initBackendAsgSecurityGroup() {
        this.backendAsgSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
            .vpc(vpc)
            .build();

        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.udp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22)); // TEST
        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(22)); // TEST
    }

    private void initBackendAsg() {
        
        initEth2NodeUserData();

        initBackendAsgSecurityGroup();

        backendAsg = AutoScalingGroup.Builder.create(this, "backendAsg")
            .vpc(vpc)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.LARGE))
            .machineImage(getMachineImage())
            .keyName("eth-stack")
            .userData(userdata)
            .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
            .init(getEth2NodeCloudInit())
            .minCapacity(MIN_GETH_INSTANCES)
            .maxCapacity(MAX_GETH_INSTANCES)
            .allowAllOutbound(Boolean.TRUE)
            .securityGroup(backendAsgSecurityGroup)
            .updatePolicy(UpdatePolicy.rollingUpdate())
            .signals(Signals.waitForMinCapacity(
                    SignalsOptions.builder().timeout(
                        Duration.minutes(Integer.valueOf(5))).build()))
            .build();

        this.userdata.addSignalOnExitCommand(backendAsg);

        // Grant the backendAsg access to attacht he volume
        this.ethVolume.grantAttachVolumeByResourceTag(
            this.backendAsg.getGrantPrincipal(), 
            Arrays.asList(this.backendAsg));
        
        this.ethVolume.grantDetachVolumeByResourceTag(
            this.backendAsg.getGrantPrincipal(), 
            Arrays.asList(this.backendAsg));
    }

    private static IMachineImage getMachineImage() {
        return MachineImage.genericLinux(
            new HashMap<String, String>(){
            {
                put("us-east-1", "ami-0db6c6238a40c0681");
                put("us-east-2", "ami-03b6c8bd55e00d5ed");
            }});
    }

    private CloudFormationInit getEth2NodeCloudInit() {
        return CloudFormationInit.fromElements(
                InitCommand.shellCommand("sudo add-apt-repository -y ppa:ethereum/ethereum"),
                InitCommand.shellCommand("sudo apt update"),
                InitCommand.shellCommand("sudo apt install awscli -y"),
                InitCommand.shellCommand("sudo apt install geth"),
                InitCommand.shellCommand("sudo mkdir -p /var/lib/goethereum"),
                // Store the volume data
                InitCommand.shellCommand("echo ${ETH_VOLUME_ID} > /home/ubuntu/eth-volume-id && echo ${ETH_VOLUME_REGION} > /home/ubuntu/eth-volume-region", 
                    InitCommandOptions.builder().env(
                        new HashMap<String, String>(){
                            {
                                put("ETH_VOLUME_ID", Eth2Stack.this.ethVolume.getVolumeId());
                                put("ETH_VOLUME_REGION", Eth2Stack.this.getRegion());
                            }
                        }).build()),
                // Attach the eth data volume
                InitCommand.shellCommand("aws ec2 attach-volume --device /dev/sdd --instance-id $(curl http://169.254.169.254/latest/meta-data/instance-id) --volume-id ${ETH_VOLUME_ID} --region ${ETH_VOLUME_REGION}",
                    InitCommandOptions.builder().env(
                        new HashMap<String, String>(){
                            {
                                put("ETH_VOLUME_ID", Eth2Stack.this.ethVolume.getVolumeId());
                                put("ETH_VOLUME_REGION", Eth2Stack.this.getRegion());
                            }
                        })
                        .build()),
                // Wait for volume to be attached
                InitCommand.shellCommand("sleep 3"),
                // Format the volume, if needed
                InitCommand.shellCommand("sudo file -s /dev/nvme1n1 | grep 'ext4' || sudo mkfs -t ext4 /dev/nvme1n1"),
                // Wait for the volume to be formatted
                InitCommand.shellCommand("sleep 5"),
                // Mount the volume
                InitCommand.shellCommand("sudo mount /dev/nvme1n1 /var/lib/goethereum"),
                InitCommand.shellCommand("sudo useradd --no-create-home --shell /bin/false goeth"),
                InitCommand.shellCommand("sudo chown -R goeth:goeth /var/lib/goethereum"),
                InitFile.fromAsset("/etc/systemd/system/geth.service", "src/main/resources/units/geth.service"),
                // InitFile.fromAsset("/etc/systemd/system/goeth-volume-attachment.service", "src/main/resources/units/goeth-volume-attachment.service"),
                // InitFile.fromAsset("/etc/systemd/system/goeth-volume.mount", "src/main/resources/units/goeth-volume.mount"),
                InitFile.fromAsset("/usr/local/bin/attach-goeth-volume.sh", "src/main/resources/bin/attach-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("ubuntu")
                        .group("ubuntu")
                        .mode("755")
                        .build()),
                InitFile.fromAsset("/usr/local/bin/detach-goeth-volume.sh", "src/main/resources/bin/detach-goeth-volume.sh", 
                        InitFileAssetOptions.builder()
                            .owner("ubuntu")
                            .group("ubuntu")
                            .mode("755")
                            .build()),
                InitFile.fromAsset("/usr/local/bin/format-goeth-volume.sh", "src/main/resources/bin/format-goeth-volume.sh", 
                        InitFileAssetOptions.builder()
                            .owner("ubuntu")
                            .group("ubuntu")
                            .mode("755")
                            .build()),
                InitFile.fromAsset("/usr/local/bin/mount-goeth-volume.sh", "src/main/resources/bin/mount-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("ubuntu")
                        .group("ubuntu")
                        .mode("755")
                        .build()),
                InitFile.fromAsset("/usr/local/bin/unmount-goeth-volume.sh", "src/main/resources/bin/unmount-goeth-volume.sh", 
                    InitFileAssetOptions.builder()
                        .owner("ubuntu")
                        .group("ubuntu")
                        .mode("755")
                        .build()),
                InitCommand.shellCommand("sudo systemctl daemon-reload"),
                InitCommand.shellCommand("sudo systemctl start geth"),
                InitCommand.shellCommand("sudo systemctl status geth")
                );
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }

    public void initEth2NodeUserData() {
        this.userdata = UserData.forLinux();
        userdata.addCommands(
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
}
