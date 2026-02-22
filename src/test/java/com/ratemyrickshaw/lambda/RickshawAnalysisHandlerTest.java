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

        // Test setters for URL
        request.setImageUrl("https://example.com/image.jpg");
        assertEquals("https://example.com/image.jpg", request.getImageUrl());

        // Test setters for base64
        request.setImageBase64("base64encodedstring");
        assertEquals("base64encodedstring", request.getImageBase64());
    }

    @Test
    public void testResponseCreation() {
        ImageAnalysisResponse response = ImageAnalysisResponse.builder().
                build();

        assertNotNull(response);

        response.setSuccess(true);
        assertTrue(response.isSuccess());

        response.setMessage("Test message");
        assertEquals("Test message", response.getMessage());
    }
}
