package com.everythingbiig.ethereum;

import org.jetbrains.annotations.NotNull;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class EthereumBeaconChainService extends Construct {

    private EthereumBeaconChainNetwork      networking = null;
    private EthereumBeaconChainNode         beaconChainNode = null;

    public EthereumBeaconChainService(software.constructs.@NotNull Construct scope, @NotNull String id) {
        super(scope, id);

        Environment deployEnv = getDeployEnvironment();
        this.networking = new EthereumBeaconChainNetwork(this, "networking", StackProps.builder()
            .env(deployEnv)
            .build());

        this.beaconChainNode = new EthereumBeaconChainNode(this, "goeth", 
            EthereumBeaconChainProps.builder()
                .beaconChainEnvironment(getBeaconChainEnvironment())
                .appVpc(this.networking.getAppVpc())
                .privateHostedZone(this.networking.getPrivateHostedZone())
                .build(), 
            StackProps.builder()
                .env(deployEnv)
            .build());
    }

    public String getBeaconChainEnvironment() {
        String beaconChainEnv = System.getenv("BEACON_CHAIN_NETWORK");
        if (!("testnet".equals(beaconChainEnv) || "mainnet".equals(beaconChainEnv))) {
            throw new IllegalArgumentException("BEACON_CHAIN_NETWORK must be one of [testnet,mainnet]");
        }
        return beaconChainEnv;
    }

    public Environment getDeployEnvironment() {
        String account = System.getenv("CDK_DEPLOY_ACCOUNT");
        if (account == null || account.trim().length() == 0) {
            account = System.getenv("CDK_DEFAULT_ACCOUNT");
            System.out.println(String.format("Falling back to CDK_DEFAULT_ACCOUNT=%s", account));
        }
        String region = System.getenv("CDK_DEPLOY_REGION");
        if (region == null || region.trim().length() == 0) {
            region = System.getenv("CDK_DEFAULT_REGION");
            System.out.println(String.format("Falling back to CDK_DEFAULT_REGION=%s", region));
        }
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}
