package com.everythingbiig.ethereum;

import org.jetbrains.annotations.NotNull;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class EthereumBeaconChainService extends Construct {

    private Networking networking = null;
    private Lighthouse lighthouse = null;

    public EthereumBeaconChainService(software.constructs.@NotNull Construct scope, @NotNull String id) {
        super(scope, id);

        this.networking = new Networking(this, "networking", StackProps.builder()
            .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
            .build());

        this.lighthouse = new  Lighthouse(this, "lighthouse", StackProps.builder()
            .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
            .build(), this.networking.getAppVpc());
    }
}
