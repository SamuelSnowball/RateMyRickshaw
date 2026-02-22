package com.ratemyrickshaw.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/*
 In Quarkus, AWS SDK clients should be injected as CDI beans or created using producers.
The issue is that the AWS SDK clients are being created in the constructor of RekognitionService. For Quarkus Lambda, we should use CDI producers to create these clients properly. Let me create a producer class and update the service to inject the clients instead of creating them in the constructor.
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
