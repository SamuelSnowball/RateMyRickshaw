package com.ratemyrickshaw.service;

import com.ratemyrickshaw.model.ImageAnalysisResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RekognitionService {

    private final RekognitionClient rekognitionClient;
    private final S3Client s3Client;

    public RekognitionService() {
        String region = System.getenv().getOrDefault("AWS_REGION", "eu-west-2");
        this.rekognitionClient = RekognitionClient.builder()
                .region(Region.of(region))
                .build();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public ImageAnalysisResponse analyzeImage(String s3Bucket, String s3Key) {
        ImageAnalysisResponse response = new ImageAnalysisResponse();
        
        try {
            Image image = Image.builder()
                    .s3Object(S3Object.builder()
                            .bucket(s3Bucket)
                            .name(s3Key)
                            .build())
                    .build();

            DetectLabelsRequest request = DetectLabelsRequest.builder()
                    .image(image)
                    .maxLabels(20)
                    .minConfidence(70F)
                    .build();

            DetectLabelsResponse result = rekognitionClient.detectLabels(request);
            
            List<Label> labels = result.labels();
            
            response.setSuccess(true);
            response.setLabels(labels.stream()
                    .map(Label::name)
                    .collect(Collectors.toList()));
            
            Map<String, Float> confidenceMap = new HashMap<>();
            for (Label label : labels) {
                confidenceMap.put(label.name(), label.confidence());
            }
            response.setLabelConfidence(confidenceMap);

            // Check if image contains rickshaw-related labels
            boolean isRickshaw = false;
            float maxConfidence = 0f;
            
            Set<String> rickshawKeywords = Set.of(
                "rickshaw", "tuk tuk", "auto rickshaw", "three wheeler",
                "vehicle", "transportation", "taxi", "car"
            );

            for (Label label : labels) {
                String labelName = label.name().toLowerCase();
                for (String keyword : rickshawKeywords) {
                    if (labelName.contains(keyword) || keyword.contains(labelName)) {
                        isRickshaw = true;
                        maxConfidence = Math.max(maxConfidence, label.confidence());
                    }
                }
            }

            response.setRickshaw(isRickshaw);
            response.setRickshawConfidence(maxConfidence);
            response.setMessage("Image analyzed successfully");

        } catch (RekognitionException e) {
            response.setSuccess(false);
            response.setMessage("Rekognition error: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error: " + e.getMessage());
        }

        return response;
    }

    public ImageAnalysisResponse analyzeImageFromUrl(String imageUrl) {
        ImageAnalysisResponse response = new ImageAnalysisResponse();
        
        try {
            // Download image from URL
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] imageBytes = outputStream.toByteArray();
            inputStream.close();

            Image image = Image.builder()
                    .bytes(SdkBytes.fromByteArray(imageBytes))
                    .build();

            DetectLabelsRequest request = DetectLabelsRequest.builder()
                    .image(image)
                    .maxLabels(20)
                    .minConfidence(70F)
                    .build();

            DetectLabelsResponse result = rekognitionClient.detectLabels(request);
            
            List<Label> labels = result.labels();
            
            response.setSuccess(true);
            response.setLabels(labels.stream()
                    .map(Label::name)
                    .collect(Collectors.toList()));
            
            Map<String, Float> confidenceMap = new HashMap<>();
            for (Label label : labels) {
                confidenceMap.put(label.name(), label.confidence());
            }
            response.setLabelConfidence(confidenceMap);

            // Check if image contains rickshaw
            boolean isRickshaw = false;
            float maxConfidence = 0f;
            
            Set<String> rickshawKeywords = Set.of(
                "rickshaw", "tuk tuk", "auto rickshaw", "three wheeler",
                "vehicle", "transportation"
            );

            for (Label label : labels) {
                String labelName = label.name().toLowerCase();
                for (String keyword : rickshawKeywords) {
                    if (labelName.contains(keyword)) {
                        isRickshaw = true;
                        maxConfidence = Math.max(maxConfidence, label.confidence());
                    }
                }
            }

            response.setRickshaw(isRickshaw);
            response.setRickshawConfidence(maxConfidence);
            response.setMessage("Image analyzed successfully");

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error: " + e.getMessage());
        }

        return response;
    }
}
