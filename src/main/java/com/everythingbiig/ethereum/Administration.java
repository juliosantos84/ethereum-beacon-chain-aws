package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.BastionHostLinux;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
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
        

        this.bastion = BastionHostLinux.Builder.create(this, id)
            .vpc(vpc)
            .subnetSelection(SubnetSelection.builder()
                .subnetType(SubnetType.PUBLIC)
                .build())
            .instanceName("Bastion")
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MICRO))
            .machineImage(MachineImage.latestAmazonLinux())
            .securityGroup(getBastionSecurityGroup())
            .build();
        
    }

    private String getBastionAllowedCidr() {
        return System.getenv("BASTION_ALLOWED_CIDR");
    }

    public ISecurityGroup getBastionSecurityGroup() {
        if(this.bastionSecurityGroup == null) {
            this.bastionSecurityGroup = SecurityGroup.Builder.create(this, "bastion")
                .vpc(this.vpc)
                .securityGroupName("bastionSecurityGroup")
                .build();

            String bastionAllowedCidr = getBastionAllowedCidr();

            if(bastionAllowedCidr != null && bastionAllowedCidr.length() > 0){
                this.bastionSecurityGroup.addIngressRule(
                    Peer.ipv4(bastionAllowedCidr), 
                    Port.tcp(Integer.valueOf(22))
                );
            }
        }
        return this.bastionSecurityGroup;
    }

    public IPeer getBastionCidr() {
        return Peer.ipv4(this.bastion.getInstancePrivateIp()+"/32");
    }
}
