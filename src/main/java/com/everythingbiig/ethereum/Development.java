package com.everythingbiig.ethereum;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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

    private EthereumStackProps ethProps = null;

    public Development(final Construct scope, final String id) {
        this(scope, id, null, null);
    }
    public Development(final Construct scope, final String id, EthereumStackProps ethProps, StackProps props) {
        super(scope, id);
        this.ethProps = ethProps;
        getImagePipeline();
    }

    public CfnImagePipeline getImagePipeline() {

        if (pipeline == null) {

            CfnDistributionConfiguration distroConfig = CfnDistributionConfiguration.Builder
                .create(this, "distroConfigDefault")
                .name("etherythingbiigDistroConfig")
                .description("Etherythingbiig Distribution Config")
                .distributions(Arrays.asList(
                    DistributionProperty.builder()
                        .amiDistributionConfiguration(new HashMap<String, Object>(){
                            {
                                put("name", "etherythingbiig-{{imagebuilder:buildVersion}}-{{imagebuilder:buildDate}}");
                            }
                        })
                        .region(Development.this.getRegion())
                        .build()))
                .build();

            CfnComponent gethComponent = CfnComponent.Builder.create(this, "gethComponent")
                .name("geth")
                .platform("Linux")
                .description("Installs geth component")
                .data(getFileAsString("/imagebuilder/geth-component.yaml"))
                .version("1.10.8")
                .build();
            
            CfnComponent gethTestComponent = CfnComponent.Builder.create(this, "gethTestComponent")
                .name("geth-test")
                .description("Tests geth component")
                .platform("Linux")
                .data(getFileAsString("/imagebuilder/geth-test-component.yaml"))
                .version("1.10.8")
                .build();

            CfnImageRecipe recipe = CfnImageRecipe.Builder.create(this, "imageRecipe")
                .name("etherythingbiigImageRecipe")
                // amzn2-ami-hvm-2.0.20211001.1-x86_64-ebs
                .parentImage("ami-0940aa04644aba71c")
                .description("Etherythingbiig Image Recipe")
                .workingDirectory("/tmp")
                .version("0.0.1")
                .components(Arrays.asList(
                    CfnImageRecipe.ComponentConfigurationProperty
                        .builder()
                        .componentArn(gethComponent.getAttrArn())
                        .build(),
                    CfnImageRecipe.ComponentConfigurationProperty
                        .builder()
                        .componentArn(gethTestComponent.getAttrArn())
                        .build()))
                .build();
            
            CfnInfrastructureConfiguration infraConfig = 
                CfnInfrastructureConfiguration.Builder.create(this, "infraConfig")
                    .name("etherythingbiigInfraConfig")
                    .description("Etherythingbiig Infrastructure Config")
                    .terminateInstanceOnFailure(Boolean.TRUE)
                    .instanceProfileName("EC2InstanceProfileForImageBuilder")
                    .build();
            pipeline = CfnImagePipeline.Builder
                .create(this, "imagePipeline")
                .name("etherythingbiigImagePipeline")
                .description("Etherythingbiig Image Pipeline")
                .distributionConfigurationArn(distroConfig.getAttrArn())
                .imageRecipeArn(recipe.getAttrArn())
                .infrastructureConfigurationArn(infraConfig.getAttrArn())
                .build();
        }
        return pipeline;
    }
    private String getFileAsString(String filePathString) {
        String fileAsString = null;
        try {
            fileAsString = new String(
                Files.readAllBytes(
                    Paths.get(
                        Development.class.getResource(filePathString).toURI())),
                        StandardCharsets.UTF_8
            );
        } catch (Exception io) {
            //TODO
            io.printStackTrace();;
        }
        return fileAsString;
    }
}
