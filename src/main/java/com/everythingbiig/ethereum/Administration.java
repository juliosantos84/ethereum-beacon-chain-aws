package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.BastionHostLinux;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;

public class Administration extends Stack {
    
    private Vpc vpc = null;
    private BastionHostLinux bastion = null;
    private ISecurityGroup bastionSecurityGroup = null;

    public Administration(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, vpc, null);
    }

    public Administration(Construct scope, String id, Vpc vpc, StackProps props) {
        super(scope, id, props);
        this.vpc = vpc;
        this.bastionSecurityGroup = SecurityGroup.Builder.create(this, "bastion")
            .vpc(this.vpc)
            .securityGroupName("bastionSecurityGroup")
            .build();

        String bastionAllowedCidr = System.getenv("BASTION_ALLOWED_CIDR");

        if(bastionAllowedCidr != null && bastionAllowedCidr.length() > 0){
            this.bastionSecurityGroup.addIngressRule(
                Peer.ipv4(bastionAllowedCidr), 
                Port.tcp(Integer.valueOf(22))
            );
        }

        this.bastion = BastionHostLinux.Builder.create(this, id)
            .vpc(vpc)
            .instanceName("Bastion")
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MICRO))
            .machineImage(MachineImage.latestAmazonLinux())
            .securityGroup(bastionSecurityGroup)
            .build();
    }

    public ISecurityGroup getBastionSecurityGroup() {
        return this.bastionSecurityGroup;
    }
}
