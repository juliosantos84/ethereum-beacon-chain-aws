package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;



public class Eth2Stack extends Stack {
    public Eth2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Eth2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Configure a VPC
        Vpc ethVpc = Vpc.Builder.create(this, "EthVpc")
            .cidr("10.1.0.0/16")
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(3))
            .build();
        
        // Configure a load balancer and ec2 ASG
        NetworkLoadBalancer publicLb = NetworkLoadBalancer.Builder.create(this, "publicLb")
            .vpc(ethVpc)
            .vpcSubnets(
                SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build()
            )
            .internetFacing(Boolean.TRUE)
            .build();

        // Go Eth Listener
        publicLb.addListener("goEth", 
            getNetworkListenerProps(Protocol.TCP_UDP, 30303)
        );

        // Lighthouse Listener
        publicLb.addListener("lighthouse", 
            getNetworkListenerProps(Protocol.TCP_UDP, 9000)
        );

        // grafana Listener
        publicLb.addListener("grafana", 
            getNetworkListenerProps(Protocol.TCP_UDP, 3000)
        );

        // Instance eth1Client = Instance.Builder.create(this, "Eth1Client")
        //     .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.LARGE))
        //     .machineImage(MachineImage.genericLinux(
        //         new HashMap<String, String>(){
        //         {
        //             put("us-east-1", "ami-0db6c6238a40c0681");
        //             put("us-east-2", "ami-03b6c8bd55e00d5ed");
        //         }

        //     }))
        //     .vpc(ethVpc)
        //     .vpcSubnets(
        //         SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build()
        //     )
        //     .build();
    }

    public static NetworkListenerProps getNetworkListenerProps(Protocol protocol, Integer port) {
       return NetworkListenerProps.builder()
            .protocol(protocol)
            .port(port)
            .build();
    }
}
