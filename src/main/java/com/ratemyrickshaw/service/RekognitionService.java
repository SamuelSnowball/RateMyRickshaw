package com.ratemyrickshaw.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

@ApplicationScoped
@Slf4j
public class RekognitionService {

    @Inject
    RekognitionClient rekognitionClient;

    public DetectTextResponse uploadToRekognition(byte[] imageBytes) {
        Image image = Image.builder()
                .bytes(SdkBytes.fromByteArray(imageBytes))
                .build();

        DetectTextRequest request = DetectTextRequest.builder()
                .image(image)
                .build();

        return rekognitionClient.detectText(request);
    }

    /**
     * Analyze image from URL
     */
    public DetectTextResponse analyzeImageFromUrl(String imageUrl) {
        try {
            // Download image from URL
            URI uri = URI.create(imageUrl);
            InputStream inputStream = uri.toURL().openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            inputStream.close();

            return uploadToRekognition(imageBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error downloading image: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze image from base64 encoded string
     */
    public DetectTextResponse analyzeImageFromBase64(String base64Image) {
        try {
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            return uploadToRekognition(imageBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error decoding base64 image: " + e.getMessage(), e);
        }
    }
}
