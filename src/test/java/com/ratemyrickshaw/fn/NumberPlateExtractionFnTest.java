package com.ratemyrickshaw.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Geometry;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

@QuarkusTest
class NumberPlateExtractionFnTest {

    @Inject
    private NumberPlateExtractionFn numberPlateExtractionFn;

    @Test
    void testPostProcessRekognitionResults() throws IOException {
        // Load JSON response from file
        Path path = Paths.get("src/test/resources/ocr_data/0_result.json");
        String jsonContent = Files.readString(path);
        
        // Create DetectTextResponse from JSON
        DetectTextResponse rekognitionResponse = DetectTextResponse.builder()
                .textDetections(parseTextDetectionsFromJson(jsonContent))
                .build();
        
        String result = numberPlateExtractionFn.apply(rekognitionResponse);
        
        assertEquals("AP13v7951", result);
    }
    
    /**
     * Helper method to parse JSON manually into TextDetection objects
     * This demonstrates one approach, though in practice you might want to use
     * a more sophisticated JSON-to-model mapping
     */
    private List<TextDetection> parseTextDetectionsFromJson(String jsonContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonContent);
        
        List<TextDetection> detections = new java.util.ArrayList<>();
        
        root.get("TextDetections").forEach(node -> {
            detections.add(
                TextDetection.builder()
                    .detectedText(node.get("DetectedText").asText())
                    .type(node.get("Type").asText())
                    .id(node.get("Id").asInt())
                    .confidence(node.get("Confidence").floatValue())
                    .geometry(parseGeometry(node.get("Geometry")))
                    .build()
            );
        });
        
        return detections;
    }
    
    private Geometry parseGeometry(JsonNode node) {
        JsonNode bbox = node.get("BoundingBox");
        return Geometry.builder()
            .boundingBox(
                BoundingBox.builder()
                    .width(bbox.get("Width").floatValue())
                    .height(bbox.get("Height").floatValue())
                    .left(bbox.get("Left").floatValue())
                    .top(bbox.get("Top").floatValue())
                    .build()
            )
            .build();
    }
}
