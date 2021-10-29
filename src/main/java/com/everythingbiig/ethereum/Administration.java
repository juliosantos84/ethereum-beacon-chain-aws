package com.everythingbiig.ethereum;

import java.util.Arrays;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.BastionHostLinux;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
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
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;

public class Administration extends Stack {
    
    private BastionHostLinux bastion = null;
    private ISecurityGroup bastionSecurityGroup = null;
    private EthereumBeaconChainProps adminProps = null;

    public Administration(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public Administration(Construct scope, String id, EthereumBeaconChainProps adminProps, StackProps props) {
        super(scope, id, props);
        
        this.adminProps = adminProps;

        getBastion(id);
    }

    private void getBastion(String id) {
        if(this.bastion == null) {
            this.bastion = BastionHostLinux.Builder.create(this, id)
                .vpc(this.adminProps.getDmzVpc())
                .subnetSelection(SubnetSelection.builder()
                    .subnetType(SubnetType.PUBLIC)
                    .build())
                .instanceName("Bastion")
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MICRO))
                .machineImage(getMachineImage())
                .securityGroup(getBastionSecurityGroup())
                .build();

            if(this.adminProps.getPublicHostedZone() != null) {
                ARecord.Builder.create(this, "bastionPublicARecord")
                    .zone(this.adminProps.getPublicHostedZone())
                    .target(RecordTarget.fromIpAddresses(this.bastion.getInstancePublicIp()))
                    .recordName("bastion.public.ethereum.everythingbiig.com")
                    .ttl(Duration.minutes(1))
                    .build();
            }

            this.adminProps.getPrivateHostedZone().addVpc(this.adminProps.getDmzVpc());

            this.bastion.getGrantPrincipal()
                .addToPrincipalPolicy(PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ec2-instance-connect:SendSSHPublicKey"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList(
                        "arn:aws:ec2:" + getRegion() + ":" + getAccount() + ":instance/*"))
                    .build());
            this.bastion.getGrantPrincipal()
                .addToPrincipalPolicy(PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ssm:StartSession"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList(
                        "arn:aws:ec2:" + getRegion() + ":" + getAccount() + ":instance/*"))
                    .build());
        }
    }

    private IMachineImage getMachineImage() {
        return MachineImage.lookup(
            LookupMachineImageProps.builder()
                .owners(Arrays.asList("amazon"))
                .name("amzn-ami-hvm-2018.03.0.20210721.0-x86_64-gp2").build());
    }

    private String getBastionAllowedCidr() {
        return System.getenv("BASTION_ALLOWED_CIDR");
    }

    private ISecurityGroup getBastionSecurityGroup() {
        if(this.bastionSecurityGroup == null) {
            this.bastionSecurityGroup = SecurityGroup.Builder.create(this, "bastion")
                .vpc(this.adminProps.getDmzVpc())
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

    public IPrincipal getAdministrationPrincipal() {
        return this.bastion.getGrantPrincipal();
    }
    
    public IPeer getAdministrationCidr() {
        return Peer.ipv4(this.bastion.getInstancePrivateIp()+"/32");
    }
}
