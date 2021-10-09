package com.everythingbiig.ethereum;

import org.jetbrains.annotations.NotNull;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class EthereumBeaconChainService extends Construct {

    private Networking      networking = null;
    private Administration  administration = null;
    private Goeth           goeth = null;
    private Lighthouse      lighthouse = null;
    // private Fargate         fargate = null;
    private Development     development = null;

    public EthereumBeaconChainService(software.constructs.@NotNull Construct scope, @NotNull String id) {
        super(scope, id);

        this.networking = new Networking(this, "networking", StackProps.builder()
            .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
            .build());


        this.administration = new Administration(this, "administration", 
            EthereumStackProps.builder()
                .publicHostedZone(this.networking.getPublicHostedZone())
                .privateHostedZone(this.networking.getPrivateHostedZone())
                .dmzVpc(this.networking.getDmzVpc())
                .build(),
            StackProps.builder()
                .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
                .build());

        this.development = new Development(this, "development", 
            EthereumStackProps.builder()
                .devVpc(this.networking.getDevVpc())
                .build(),
            StackProps.builder()
                .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
                .build());
        // this.fargate = new Fargate(this, "fargate", 
        //     EthereumStackProps.builder()
        //         .appVpc(this.networking.getAppVpc())
        //         .publicHostedZone(this.networking.getPublicHostedZone())
        //         .privateHostedZone(this.networking.getPrivateHostedZone())
        //         .build(), 
        //     StackProps.builder()
        //         .env(Environment.builder()
        //             .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
        //             .region(System.getenv("CDK_DEFAULT_REGION"))
        //             .build())
        //         .build());

        this.goeth = new Goeth(this, "goeth", 
            EthereumStackProps.builder()
                .appVpc(this.networking.getAppVpc())
                .privateHostedZone(this.networking.getPrivateHostedZone())
                .administrationPrincipal(this.administration.getAdministrationPrincipal())
                .administrationCidr(this.administration.getAdministrationCidr())
                // .cluster(this.fargate)
                .build(), 
            StackProps.builder()
                .env(Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build())
            .build());

        this.lighthouse = new  Lighthouse(this, "lighthouse", 
            EthereumStackProps.builder()
                .appVpc(this.networking.getAppVpc())
                .privateHostedZone(this.networking.getPrivateHostedZone())
                .administrationCidr(this.administration.getAdministrationCidr())
                .build(), 
            StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
            .build());
    }
}
