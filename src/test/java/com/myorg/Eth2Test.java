package com.myorg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.everythingbiig.ethereum.Eth2Stack;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

import software.amazon.awscdk.core.App;

public class Eth2Test {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() throws IOException {
        App app = new App();
        Eth2Stack stack = new Eth2Stack(app, "test");

        // synthesize the stack to a CloudFormation template
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

        // Update once resources have been added to the stack
        assertThat(actual.get("Resources")).isNull();
    }
}
