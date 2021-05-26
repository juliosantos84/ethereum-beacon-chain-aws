package com.myorg;

import java.util.Arrays;
import java.util.HashMap;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
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

    public Eth2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Eth2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Configure a VPC
        Vpc vpc = Vpc.Builder.create(this, "EthVpc")
            .cidr("10.1.0.0/16")
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(3))
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

        backendAsgSecurityGroup.addIngressRule(Peer.ipv4("10.1.0.0/16"), Port.tcp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(Peer.ipv4("10.1.0.0/16"), Port.udp(GOETH_PORT));
        backendAsgSecurityGroup.addIngressRule(Peer.ipv4("10.1.0.0/16"), Port.tcp(80)); // TEST

        // Autoscaling group for ETH backend
        AutoScalingGroup backendAsg = AutoScalingGroup.Builder.create(this, "backendAsg")
            .vpc(vpc)
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.LARGE))
            .desiredCapacity(Integer.valueOf(1))
            .minCapacity(Integer.valueOf(1))
            .allowAllOutbound(Boolean.TRUE)
            .securityGroup(backendAsgSecurityGroup)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build())
            .machineImage(MachineImage.genericLinux(
                new HashMap<String, String>(){
                {
                    put("us-east-1", "ami-0db6c6238a40c0681");
                    put("us-east-2", "ami-03b6c8bd55e00d5ed");
                }
            }))
            .build();

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
                .port(Integer.valueOf(80))
                .build()
        );
        
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }
}
