package com.ratemyrickshaw.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;

import com.ratemyrickshaw.model.ImageAnalysisResponse;

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


    public ImageAnalysisResponse detectTextInImage(byte[] imageBytes) {
        Image image = Image.builder()
                .bytes(SdkBytes.fromByteArray(imageBytes))
                .build();

        DetectTextRequest request = DetectTextRequest.builder()
                .image(image)
                .build();

        DetectTextResponse response = rekognitionClient.detectText(request);

        // Filter to only WORD-level detections to avoid duplicates (LINE detections contain the same text)
        response.textDetections().stream()
                .filter(textDetection -> "WORD".equals(textDetection.type().toString()))
                .forEach(textDetection -> {
                    log.info("Detected text: {} (confidence: {}, type: {})", 
                            textDetection.detectedText(), 
                            textDetection.confidence(),
                            textDetection.type());

                            // Find cerntral point of bounding box - if thats within another bounding box by some margin on all sides - assume its the same detection
                            // and take the one with the highest confidence
                            textDetection.geometry().boundingBox();
                });

        return ImageAnalysisResponse.builder()
                .build();
    }

    /**
     * Analyze image from URL
     */
    public ImageAnalysisResponse analyzeImageFromUrl(String imageUrl) {
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

            return detectTextInImage(imageBytes);

        } catch (Exception e) {
            ImageAnalysisResponse response = ImageAnalysisResponse.builder()
                    .success(false)
                    .message("Error downloading image: " + e.getMessage()).build();

            return response;
        }
    }

    /**
     * Analyze image from base64 encoded string
     */
    public ImageAnalysisResponse analyzeImageFromBase64(String base64Image) {
        try {
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            return detectTextInImage(imageBytes);

        } catch (Exception e) {
            return ImageAnalysisResponse.builder()
                    .success(false)
                    .message("Error decoding base64 image: " + e.getMessage())
                    .build();
        }
    }
}
