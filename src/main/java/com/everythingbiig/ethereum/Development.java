package com.everythingbiig.ethereum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.imagebuilder.CfnComponent;
import software.amazon.awscdk.services.imagebuilder.CfnDistributionConfiguration;
import software.amazon.awscdk.services.imagebuilder.CfnDistributionConfiguration.DistributionProperty;
import software.amazon.awscdk.services.imagebuilder.CfnImagePipeline;
import software.amazon.awscdk.services.imagebuilder.CfnImageRecipe;
import software.amazon.awscdk.services.imagebuilder.CfnInfrastructureConfiguration;

public class Development extends Stack {

    private CfnImagePipeline pipeline = null;

    public Development(final Construct scope, final String id) {
        this(scope, id, null, null);
    }
    public Development(final Construct scope, final String id, EthereumStackProps ethProps, StackProps props) {
        getImagePipeline(scope);
    }

    public CfnImagePipeline getImagePipeline(final Construct scope) {

        if (pipeline == null) {

            CfnDistributionConfiguration distroConfig = CfnDistributionConfiguration.Builder
                .create(scope, "distroConfigDefault")
                .description("Distribute to default region.")
                .distributions(Arrays.asList(
                    DistributionProperty.builder()
                        .amiDistributionConfiguration(new HashMap<String, Object>(){
                            {
                                put("name", "etherythingbiig-{{imagebuilder:buildVersion}}");
                            }
                        })
                        .region(Development.this.getRegion())
                        .build()))
                .build();

            CfnImageRecipe recipe = CfnImageRecipe.Builder.create(scope, "imageRecipe")
                // amzn2-ami-hvm-2.0.20211001.1-x86_64-ebs
                .parentImage("ami-0940aa04644aba71c")
                .description("Builds an image that includes all the binaries to run an ETH2 stack..")
                .workingDirectory("/tmp")
                .components(Arrays.asList(
                    CfnComponent.Builder.create(scope, "gethComponent")
                        .supportedOsVersions(Arrays.asList("Amazon Linux 2"))
                        .data(getFileAsString("imagebuilder/geth-component.yaml"))
                        .build(),
                    CfnComponent.Builder.create(scope, "gethTestComponent")
                        .supportedOsVersions(Arrays.asList("Amazon Linux 2"))
                        .data(getFileAsString("imagebuilder/geth-test-component.yaml"))
                        .build()
                ))
                .build();
            CfnInfrastructureConfiguration infraConfig = 
                CfnInfrastructureConfiguration.Builder.create(scope, "infraConfig")
                    .terminateInstanceOnFailure(Boolean.TRUE)
                    .instanceProfileName("EC2InstanceProfileForImageBuilder")
                    .build();
            pipeline = CfnImagePipeline.Builder
                .create(scope, "imagePipeline")
                .distributionConfigurationArn(distroConfig.getAttrArn())
                .imageRecipeArn(recipe.getAttrArn())
                .infrastructureConfigurationArn(infraConfig.getAttrArn())
                .build();
        }
        return pipeline;
    }
    private String getFileAsString(String string) {
        String fileAsString = null;
        try {
            fileAsString = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("imagebuilder/geth-component.yaml")))
        } catch (IOException io) {
            io.printStackTrace();;
            //TODO
        }
        return fileAsString;
    }
}
