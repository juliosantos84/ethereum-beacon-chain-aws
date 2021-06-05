package com.everythingbiig.ethereum;

import java.util.Arrays;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

/**
 * Defines a networking stack based on tiers.
 * 
 * Using a CIDR of 10.1.0.0/22 to define VPCs with up to 1024 hosts.
 */
public class Networking extends Stack {
    
    // IGWs, Loadbalancers, NATs, Bastions
    private Vpc dmzVpc = null;

    // Apps and Services
    private Vpc appVpc = null;

    // Storage
    private Vpc storageVpc = null;

    public Networking(final Construct scope, final String id) {
        super(scope, id);

        // 1024 hosts
        this.dmzVpc = Vpc.Builder.create(this, "dmzVpc")
            .cidr("10.1.0.0/22")
            .subnetConfiguration(Vpc.DEFAULT_SUBNETS)
            .maxAzs(Integer.valueOf(3))
            .build();

        // 1024 hosts
        this.appVpc = Vpc.Builder.create(this, "appVpc")
            .cidr("10.1.4.0/22")
            .subnetConfiguration(
                Arrays.asList(
                    SubnetConfiguration.builder().subnetType(SubnetType.PRIVATE).build()))
            .maxAzs(Integer.valueOf(3))
            .build();

        // 1024 hosts
        this.storageVpc = Vpc.Builder.create(this, "storageVpc")
            .cidr("10.1.8.0/22")
            .subnetConfiguration(
                Arrays.asList(
                    SubnetConfiguration.builder().subnetType(SubnetType.PRIVATE).build()))
            .maxAzs(Integer.valueOf(3))
            .build();
    }

    public Vpc getDmzVpc() {
        return this.dmzVpc;
    }

    public Vpc getAppVpc() {
        return this.appVpc;
    }

    public Vpc getStoragVpc() {
        return this.storageVpc;
    }
}
