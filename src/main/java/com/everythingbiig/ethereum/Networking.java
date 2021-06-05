package com.everythingbiig.ethereum;

import java.util.Arrays;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

/**
 * Defines a networking stack based on tiers.
 * 
 * Using a CIDR of 10.1.0.0/22 to define VPCs with up to 1024 hosts.
 */
public class Networking extends Stack {
    
    public static final Integer AZ_COUNT = Integer.valueOf(2);

    // IGWs, Loadbalancers, NATs, Bastions
    private Vpc dmzVpc = null;

    // Apps and Services
    private Vpc appVpc = null;

    // Storage
    private Vpc storageVpc = null;

    public Networking(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Networking(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        getDmzVpc();

        getAppVpc();

        // getStorageVpc()
    }
    public Vpc getDmzVpc() {
        if(this.dmzVpc == null) {
            // 1024 hosts
            this.dmzVpc = Vpc.Builder.create(this, "dmzVpc")
                .cidr("10.1.0.0/22")
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
                .cidr("10.1.4.0/22")
                .subnetConfiguration(
                    Arrays.asList(
                        SubnetConfiguration.builder()
                            .name("privateAppSubnet")
                            .subnetType(SubnetType.PRIVATE).build(),
                        SubnetConfiguration.builder()
                            .name("publicAppSubnet")
                            .subnetType(SubnetType.PUBLIC).build()))
                .natGateways(AZ_COUNT)
                .maxAzs(Integer.valueOf(AZ_COUNT))
                .build();
        }
        return this.appVpc;
    }

    public Vpc getStoragVpc() {
        if(this.storageVpc == null) {
            // 1024 hosts
            this.storageVpc = Vpc.Builder.create(this, "storageVpc")
            .cidr("10.1.8.0/22")
            .subnetConfiguration(
                Arrays.asList(
                    SubnetConfiguration.builder().subnetType(SubnetType.PRIVATE).build()))
            .maxAzs(Integer.valueOf(AZ_COUNT))
            .build();
        }
        return this.storageVpc;
    }
}
