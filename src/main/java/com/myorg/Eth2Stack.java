package com.myorg;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.ApplyCloudFormationInitOptions;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.Signals;
import software.amazon.awscdk.services.autoscaling.SignalsOptions;
import software.amazon.awscdk.services.ec2.CloudFormationInit;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InitFile;
import software.amazon.awscdk.services.ec2.InitPackage;
import software.amazon.awscdk.services.ec2.InitService;
import software.amazon.awscdk.services.ec2.InitUser;
import software.amazon.awscdk.services.ec2.InitUserOptions;
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
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;



public class Eth2Stack extends Stack {

    public static final Integer GOETH_PORT = Integer.valueOf(30303);
    public static final Integer LIGHTHOUSE_PORT = Integer.valueOf(9000);
    public static final Integer GRAFANA_PORT = Integer.valueOf(3000);

    public static final String  VPC_CIDR = "10.1.0.0/16";
    public static final Integer MIN_GETH_INSTANCES = Integer.valueOf(0);
    public static final Integer MAX_GETH_INSTANCES = Integer.valueOf(1);

    public static final IPeer   VPC_CIDR_PEER = Peer.ipv4(VPC_CIDR);

    public Eth2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Eth2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Configure a VPC
        Vpc vpc = Vpc.Builder.create(this, "EthVpc")
            .cidr(VPC_CIDR)
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(1))
            .build();

        // Configure a load balancer and ec2 ASG
        NetworkLoadBalancer publicLb = NetworkLoadBalancer.Builder.create(this, "publicLb")
            .vpc(vpc)
            .vpcSubnets(
                SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build()
            )
            .internetFacing(Boolean.TRUE)
            .crossZoneEnabled(Boolean.TRUE)
            .build();

        SecurityGroup backendAsgSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
            .vpc(vpc)
            .build();

        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.udp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22)); // TEST
        backendAsgSecurityGroup.addIngressRule(VPC_CIDR_PEER, Port.tcp(22)); // TEST

        // User data to bootstrap the instance
        UserData userdata = getEth2NodeUserData();
        
        // Autoscaling group for ETH backend
        AutoScalingGroup backendAsg = AutoScalingGroup.Builder.create(this, "backendAsg")
            .vpc(vpc)
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.LARGE))
            // .desiredCapacity(Integer.valueOf(0))
            .minCapacity(MIN_GETH_INSTANCES)
            .desiredCapacity(Integer.valueOf(1))
            .maxCapacity(MAX_GETH_INSTANCES)
            .allowAllOutbound(Boolean.TRUE)
            .securityGroup(backendAsgSecurityGroup)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
            // .signals(Signals.waitForMinCapacity())
            .machineImage(MachineImage.genericLinux(
                new HashMap<String, String>(){
                {
                    put("us-east-1", "ami-0db6c6238a40c0681");
                    put("us-east-2", "ami-03b6c8bd55e00d5ed");
                }}))
            .init(getEth2NodeCloudInit())
            .signals(Signals.waitForMinCapacity(
                    SignalsOptions.builder().timeout(
                        Duration.minutes(Integer.valueOf(1))).build()))
            .userData(userdata)
            .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
            .build();
        
        userdata.addSignalOnExitCommand(backendAsg);

        NetworkListener goEthListener = publicLb.addListener("goEth", 
            NetworkListenerProps.builder()
                .protocol(Protocol.TCP_UDP)
                .port(GOETH_PORT)
                .loadBalancer(publicLb)
                .build()
        );        

        NetworkListener testListener = publicLb.addListener("test", 
            NetworkListenerProps.builder()
                .protocol(Protocol.TCP_UDP)
                .port(Integer.valueOf(80))
                .loadBalancer(publicLb)
                .build()
        );    

        NetworkTargetGroup goEthTargetGroup = goEthListener.addTargets("ethBackendTargets", 
            AddNetworkTargetsProps.builder()
                .targets(Arrays.asList(backendAsg))
                .port(GOETH_PORT)
                .build()
        );

        NetworkTargetGroup testTargetGroup = testListener.addTargets("testTargets", 
            AddNetworkTargetsProps.builder()
                .targets(Arrays.asList(backendAsg))
                .port(Integer.valueOf(22))
                .build()
        );
        
    }

    private static CloudFormationInit getEth2NodeCloudInit() {
        return CloudFormationInit.fromElements(
                // InitFile.fromUrl("aws-cfn.tar.gz", "https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-py3-latest.tar.gz"),
                // InitPackage.apt("aws-cfn-bootstrap"),
                // InitCommand.shellCommand("sudo add-apt-repository -y ppa:ethereum/ethereum"),
                // InitCommand.shellCommand("sudo apt update"),
                // InitCommand.shellCommand("sudo apt install geth"),
                InitPackage.apt("geth"),
                // InitCommand.shellCommand("sudo useradd --no-create-home --shell /bin/false goeth"),
                InitCommand.shellCommand("sudo mkdir -p /var/lib/goethereum"),
                InitUser.fromName("goeth", 
                    InitUserOptions.builder()
                        .groups(Arrays.asList("goeth")).build()),
                InitCommand.shellCommand("sudo chown -R goeth:goeth /var/lib/goethereum"),
                InitFile.fromAsset("/etc/systemd/system/geth.service", "geth.service"),
                InitService.enable("geth")
                // InitCommand.shellCommand("sudo systemctl daemon-reload"),
                // InitCommand.shellCommand("sudo systemctl start geth"),
                // InitCommand.shellCommand("/opt/aws/bin/cfn-signal -e 0 --stack ${AWS::StackId} --resource ${ASG_RESOURCE} --region ${AWS::Region}", 
                //     InitCommandOptions.builder()
                //         .env(new HashMap<String, String>(){
                //             {
                //                 put("ASG_RESOURCE", "backendAsg");
                //             }
                //         })
                //         .build())
                // InitCommand.shellCommand("sudo systemctl status geth")
                );
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }

    public UserData getEth2NodeUserData() {
        UserData userdata = UserData.forLinux();
        userdata.addCommands(
            "curl https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-py3-latest.tar.gz --output /tmp/aws-cfn-bootstrap-py3-latest.tar.gz",
            "tar -xvf /tmp/aws-cfn-bootstrap-py3-latest.tar.gz -C /tmp/",
            "mv /tmp/aws-cfn-bootstrap-2.0 /opt/aws",
            "sudo +x /opt/aws/bin",
            "cd /opt/aws",
            "sudo python3 setup.py install",
            // "curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py",
            // "python3 get-pip.py",
            "ln -s /opt/aws/init/ubuntu/cfn-hup /etc/init.d/cfn-hup",
            "sudo add-apt-repository -y ppa:ethereum/ethereum",
            "sudo apt update");
        return userdata;
    }
}
