package com.everythingbiig.ethereum;

import java.util.Arrays;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.CfnRoute;
import software.amazon.awscdk.services.ec2.CfnVPCPeeringConnection;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.route53.PrivateHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;

/**
 * Defines a networking stack based on tiers.
 * 
 * Using a CIDR of 10.1.0.0/22 to define VPCs with up to 1024 hosts.
 */
public class Networking extends Stack {
    
    public static final Integer AZ_COUNT = Integer.valueOf(2);

    // IGWs, Loadbalancers, NATs, Bastions
    private Vpc dmzVpc = null;

    // Public DNS public.ethereum.everythingbiig.com
    private PublicHostedZone publicHostedZone = null;

    // Apps and Services
    private Vpc appVpc = null;

    // Private DNS internal.ethereum.everythingbiig.com
    private PrivateHostedZone privateHostedZone = null;


    public Networking(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Networking(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        getDmzVpc();

        getAppVpc();

        getPrivateHostedZone();

        if(shouldEnableBastionAccess()) {
            allowDmzVpcToAppVpcRouting();
        }
    }

    private void allowDmzVpcToAppVpcRouting() {
        CfnVPCPeeringConnection dmz2AppPeerConn = CfnVPCPeeringConnection.Builder.create(this, "dmz2App")
            .vpcId(dmzVpc.getVpcId())
            .peerOwnerId(getAccount())
            .peerRegion(getRegion())
            .peerVpcId(appVpc.getVpcId())
            .build();

        // enable dmz public -> app private
        for (ISubnet dmzPublic : this.getDmzVpc().getPublicSubnets()) {
            CfnRoute.Builder.create(this, "dmzPubToAppPriv"+dmzPublic.getAvailabilityZone())
                .vpcPeeringConnectionId(dmz2AppPeerConn.getRef())
                .routeTableId(dmzPublic.getRouteTable().getRouteTableId())
                .destinationCidrBlock(appVpc.getVpcCidrBlock())
                .build();
        }

        // enable app private -> dmz public
        for (ISubnet appPrivate : this.getAppVpc().getPrivateSubnets()) {
            CfnRoute.Builder.create(this, "appPrivToDmzPub"+appPrivate.getAvailabilityZone())
                .vpcPeeringConnectionId(dmz2AppPeerConn.getRef())
                .routeTableId(appPrivate.getRouteTable().getRouteTableId())
                .destinationCidrBlock(dmzVpc.getVpcCidrBlock())
                .build();
        }
        
    }

    public Vpc getDmzVpc() {
        if(this.dmzVpc == null) {
            // 1024 hosts
            this.dmzVpc = Vpc.Builder.create(this, "dmzVpc")
                .cidr((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:dmzVpcCidr"))
                .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
                .maxAzs(Integer.valueOf(AZ_COUNT))
                .build();
        }
        return this.dmzVpc;
    }

    public Vpc getAppVpc() {
        if(this.appVpc == null) {
            // 1024 hosts
            this.appVpc = Vpc.Builder.create(this, "appVpc")
                .cidr((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:appVpcCidr"))
                .subnetConfiguration(
                    Arrays.asList(
                        SubnetConfiguration.builder()
                            .name("privateAppSubnet")
                            .subnetType(SubnetType.PRIVATE).build(),
                        SubnetConfiguration.builder()
                            .name("publicAppSubnet")
                            .subnetType(SubnetType.PUBLIC).build()))
                .natGateways(AZ_COUNT)
                .maxAzs((Integer) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:azCount"))
                .build();
        }
        return this.appVpc;
    }

    public PrivateHostedZone getPrivateHostedZone() {
        if( this.privateHostedZone == null) {
            this.privateHostedZone = PrivateHostedZone.Builder.create(this, "privateHostedZone")
                .vpc(this.getAppVpc()) // We need a default, so it will be the app vpc
                .zoneName((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:privateHostedZone"))
                .build();
        }
        return this.privateHostedZone;
    }

    public PublicHostedZone getPublicHostedZone() {
        if( this.publicHostedZone == null) {
            this.publicHostedZone = PublicHostedZone.Builder.create(this, "publicHostedZone")
                .zoneName((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:publicHostedZone"))
                .build();
        }
        return this.publicHostedZone;
    }

    protected Boolean shouldEnableBastionAccess() {
        return (Boolean) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableBastionAccess");
    }
}
