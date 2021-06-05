package com.everythingbiig.ethereum;

import org.jetbrains.annotations.NotNull;

import software.amazon.awscdk.core.Construct;

public class EthereumBeaconChainService extends Construct {

    private Networking networking = null;
    public EthereumBeaconChainService(software.constructs.@NotNull Construct scope, @NotNull String id) {
        super(scope, id);

        this.networking = new Networking(this, "networking");
        new Lighthouse(this, "lighthouse", this.networking.getAppVpc());
    }
}
