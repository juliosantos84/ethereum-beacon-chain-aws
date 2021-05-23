package com.myorg;

import java.util.HashMap;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

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
        

        // The code that defines your stack goes here
        Instance eth1Client = Instance.Builder.create(this, "Eth1Client")
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.LARGE))
            .machineImage(MachineImage.genericLinux(
                new HashMap<String, String>(){
                {
                    put("us-east-1", "ami-0db6c6238a40c0681");
                    put("us-east-2", "ami-03b6c8bd55e00d5ed");
                }

            }))
            // .availabilityZone("us-east-1a")
            .vpc(ethVpc)
            .vpcSubnets(
                // SubnetSelection.builder().subnets(
                //     ethVpc.getPrivateSubnets()
                // ).onePerAz(true).build()
                SubnetSelection.builder().subnetType(SubnetType.PRIVATE).build()
            )
            .build();
    }
}
