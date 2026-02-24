package com.ratemyrickshaw.bean;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/*
In Quarkus, AWS SDK clients should be injected as CDI beans or created using producers.
*/
@ApplicationScoped
public class AwsClientProducer {

    @Produces
    @ApplicationScoped
    public RekognitionClient rekognitionClient() {
        String region = System.getenv().getOrDefault("AWS_REGION", "eu-west-2");
        // AWS SDK will automatically use apache-client from classpath
        return RekognitionClient.builder()
                .region(Region.of(region))
                .build();
    }
}
