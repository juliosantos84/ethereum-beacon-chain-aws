package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.eks.EndpointAccess;
import software.amazon.awscdk.services.eks.FargateCluster;
import software.amazon.awscdk.services.eks.KubernetesVersion;

public class Fargate extends Stack {

    private EthereumStackProps ethProps = null;
    private FargateCluster cluster = null;

    public Fargate(Construct scope, String id) {
        this(scope, id, null, null);
    }
    
    public Fargate(Construct scope, String id, EthereumStackProps ethProps, StackProps props) {
        super(scope, id, props);

        this.ethProps = ethProps;

        getCluster();
    }
    
    public FargateCluster getCluster() {
        if(cluster == null) {
            cluster = FargateCluster.Builder.create(this, "ethereumBeaconChain")
                .endpointAccess(EndpointAccess.PRIVATE)
                .vpc(ethProps.getAppVpc())
                .placeClusterHandlerInVpc(Boolean.TRUE)
                .outputConfigCommand(Boolean.TRUE)
                .outputClusterName(Boolean.TRUE)
                .version(KubernetesVersion.V1_19)
                .build();
        }
        return cluster;
    }
}
