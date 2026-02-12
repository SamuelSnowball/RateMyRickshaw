package com.ratemyrickshaw.lambda;

import com.ratemyrickshaw.model.ImageAnalysisRequest;
import com.ratemyrickshaw.model.ImageAnalysisResponse;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RickshawAnalysisHandlerTest {

    @Test
    public void testRequestValidation() {
        ImageAnalysisRequest request = new ImageAnalysisRequest();
        
        // Test that request can be created
        assertNotNull(request);
        
        // Test setters
        request.setImageUrl("https://example.com/image.jpg");
        assertEquals("https://example.com/image.jpg", request.getImageUrl());
        
        request.setS3Bucket("test-bucket");
        assertEquals("test-bucket", request.getS3Bucket());
        
        request.setS3Key("test-key.jpg");
        assertEquals("test-key.jpg", request.getS3Key());
    }

    @Test
    public void testResponseCreation() {
        ImageAnalysisResponse response = new ImageAnalysisResponse();
        
        assertNotNull(response);
        
        response.setSuccess(true);
        assertTrue(response.isSuccess());
        
        response.setMessage("Test message");
        assertEquals("Test message", response.getMessage());
        
        response.setRickshaw(true);
        assertTrue(response.isRickshaw());
        
        response.setRickshawConfidence(95.5f);
        assertEquals(95.5f, response.getRickshawConfidence(), 0.01);
    }
}
