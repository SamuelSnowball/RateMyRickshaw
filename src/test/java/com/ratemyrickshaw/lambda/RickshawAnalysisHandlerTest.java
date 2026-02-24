package com.ratemyrickshaw.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratemyrickshaw.model.ImageAnalysisRequest;
import com.ratemyrickshaw.model.ImageAnalysisResponse;
import com.ratemyrickshaw.service.PostRekognitionService;
import com.ratemyrickshaw.service.RekognitionService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

import java.util.List;

@QuarkusTest
class RickshawAnalysisHandlerTest {

    @Inject
    RickshawAnalysisHandler handler;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    RekognitionService rekognitionService;

    @InjectMock
    PostRekognitionService postRekognitionService;

    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    void testHandleRequest_WithImageUrl_Success() throws Exception {
        // Given
        String imageUrl = "https://example.com/image.jpg";
        ImageAnalysisRequest request = new ImageAnalysisRequest(imageUrl, null);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));
        
        DetectTextResponse mockDetectResponse = DetectTextResponse.builder()
                .textDetections(List.of(
                    TextDetection.builder()
                        .detectedText("ABC123")
                        .confidence(95.5f)
                        .build()
                ))
                .build();
        
        when(rekognitionService.analyzeImageFromUrl(imageUrl))
                .thenReturn(mockDetectResponse);
        when(postRekognitionService.postProcessTextDetections(mockDetectResponse))
                .thenReturn("ABC123");

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(200, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertTrue(response.isSuccess());
        assertEquals("Image analysis completed successfully", response.getMessage());
        assertEquals("ABC123", response.getData());
        
        verify(rekognitionService).analyzeImageFromUrl(imageUrl);
        verify(rekognitionService, never()).analyzeImageFromBase64(any());
        verify(postRekognitionService).postProcessTextDetections(mockDetectResponse);
    }

    @Test
    void testHandleRequest_WithBase64Image_Success() throws Exception {
        // Given
        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD...";
        ImageAnalysisRequest request = new ImageAnalysisRequest(null, base64Image);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));
        
        DetectTextResponse mockDetectResponse = DetectTextResponse.builder()
                .textDetections(List.of(
                    TextDetection.builder()
                        .detectedText("XYZ789")
                        .confidence(92.3f)
                        .build()
                ))
                .build();
        
        when(rekognitionService.analyzeImageFromBase64(base64Image))
                .thenReturn(mockDetectResponse);
        when(postRekognitionService.postProcessTextDetections(mockDetectResponse))
                .thenReturn("XYZ789");

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(200, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertTrue(response.isSuccess());
        assertEquals("Image analysis completed successfully", response.getMessage());
        assertEquals("XYZ789", response.getData());
        
        verify(rekognitionService).analyzeImageFromBase64(base64Image);
        verify(rekognitionService, never()).analyzeImageFromUrl(any());
        verify(postRekognitionService).postProcessTextDetections(mockDetectResponse);
    }

    @Test
    void testHandleRequest_WithBothUrlAndBase64_PreferUrl() throws Exception {
        // Given - when both are provided, URL should take precedence
        String imageUrl = "https://example.com/image.jpg";
        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD...";
        ImageAnalysisRequest request = new ImageAnalysisRequest(imageUrl, base64Image);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));
        
        DetectTextResponse mockDetectResponse = DetectTextResponse.builder().build();
        
        when(rekognitionService.analyzeImageFromUrl(imageUrl))
                .thenReturn(mockDetectResponse);
        when(postRekognitionService.postProcessTextDetections(mockDetectResponse))
                .thenReturn("ABC123");

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertTrue(response.isSuccess());
        verify(rekognitionService).analyzeImageFromUrl(imageUrl);
        verify(rekognitionService, never()).analyzeImageFromBase64(any());
    }

    @Test
    void testHandleRequest_WithNoImageProvided_ReturnsError() throws Exception {
        // Given - neither URL nor base64 provided
        ImageAnalysisRequest request = new ImageAnalysisRequest(null, null);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(400, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertFalse(response.isSuccess());
        assertEquals("Either imageUrl or imageBase64 must be provided", response.getMessage());
        
        // Verify no service calls were made
        verify(rekognitionService, never()).analyzeImageFromUrl(any());
        verify(rekognitionService, never()).analyzeImageFromBase64(any());
        verify(postRekognitionService, never()).postProcessTextDetections(any());
    }

    @Test
    void testHandleRequest_WithEmptyStrings_ReturnsError() throws Exception {
        // Given - empty strings for both
        ImageAnalysisRequest request = new ImageAnalysisRequest("", "");
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(400, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertFalse(response.isSuccess());
        assertEquals("Either imageUrl or imageBase64 must be provided", response.getMessage());
        
        verify(rekognitionService, never()).analyzeImageFromUrl(any());
        verify(rekognitionService, never()).analyzeImageFromBase64(any());
    }

    @Test
    void testHandleRequest_RekognitionServiceThrowsException_ReturnsError() throws Exception {
        // Given
        String imageUrl = "https://example.com/invalid-image.jpg";
        ImageAnalysisRequest request = new ImageAnalysisRequest(imageUrl, null);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));
        
        when(rekognitionService.analyzeImageFromUrl(imageUrl))
                .thenThrow(new RuntimeException("Failed to download image"));

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(500, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Error processing image"));
        assertTrue(response.getMessage().contains("Failed to download image"));
        
        verify(rekognitionService).analyzeImageFromUrl(imageUrl);
        verify(postRekognitionService, never()).postProcessTextDetections(any());
    }

    @Test
    void testHandleRequest_PostProcessingThrowsException_ReturnsError() throws Exception {
        // Given
        String imageUrl = "https://example.com/image.jpg";
        ImageAnalysisRequest request = new ImageAnalysisRequest(imageUrl, null);
        
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(request));
        
        DetectTextResponse mockDetectResponse = DetectTextResponse.builder().build();
        
        when(rekognitionService.analyzeImageFromUrl(imageUrl))
                .thenReturn(mockDetectResponse);
        when(postRekognitionService.postProcessTextDetections(mockDetectResponse))
                .thenThrow(new RuntimeException("Post-processing failed"));

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(500, responseEvent.getStatusCode());
        
        ImageAnalysisResponse response = objectMapper.readValue(responseEvent.getBody(), ImageAnalysisResponse.class);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Error processing image"));
        assertTrue(response.getMessage().contains("Post-processing failed"));
        
        verify(rekognitionService).analyzeImageFromUrl(imageUrl);
        verify(postRekognitionService).postProcessTextDetections(mockDetectResponse);
    }

    @Test
    void testHandleRequest_OptionsRequest_ReturnsOk() {
        // Given - OPTIONS preflight request
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
                .withHttpMethod("OPTIONS");

        // When
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, mockContext);

        // Then
        assertNotNull(responseEvent);
        assertEquals(200, responseEvent.getStatusCode());
        assertTrue(responseEvent.getHeaders().containsKey("Access-Control-Allow-Origin"));
        assertTrue(responseEvent.getHeaders().containsKey("Access-Control-Allow-Methods"));
        assertTrue(responseEvent.getHeaders().containsKey("Access-Control-Allow-Headers"));
        
        // Verify no service calls were made for OPTIONS
        verify(rekognitionService, never()).analyzeImageFromUrl(any());
        verify(rekognitionService, never()).analyzeImageFromBase64(any());
        verify(postRekognitionService, never()).postProcessTextDetections(any());
    }
}
