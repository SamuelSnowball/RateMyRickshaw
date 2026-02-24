package com.ratemyrickshaw.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratemyrickshaw.model.ImageAnalysisRequest;
import com.ratemyrickshaw.model.ImageAnalysisResponse;
import com.ratemyrickshaw.service.PostRekognitionService;
import com.ratemyrickshaw.service.RekognitionService;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;

@Slf4j
@Named("rickshawAnalysis")
@RequiredArgsConstructor
public class RickshawAnalysisHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final RekognitionService rekognitionService;
    private final PostRekognitionService postRekognitionService;
    private final ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        log.info("Processing request with method: {}", requestEvent.getHttpMethod());

        try {
            // Parse the request body
            ImageAnalysisRequest request = objectMapper.readValue(requestEvent.getBody(), ImageAnalysisRequest.class);
            log.info("Processing image analysis request");

            DetectTextResponse detectTextResponse;

            if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
                log.info("Analyzing image from URL: {}", request.getImageUrl());
                detectTextResponse = rekognitionService.analyzeImageFromUrl(request.getImageUrl());
            } else if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
                log.info("Analyzing image from base64 data");
                detectTextResponse = rekognitionService.analyzeImageFromBase64(request.getImageBase64());
            } else {
                ImageAnalysisResponse errorResponse = ImageAnalysisResponse.builder()
                        .success(false)
                        .message("Either imageUrl or imageBase64 must be provided")
                        .build();
                return createResponse(400, errorResponse);
            }

            // Process the detection response
            String detectedText = postRekognitionService.postProcessTextDetections(detectTextResponse);

            ImageAnalysisResponse response = ImageAnalysisResponse.builder()
                    .success(true)
                    .message("Image analysis completed successfully")
                    .data(detectedText)
                    .build();

            return createResponse(200, response);

        } catch (Exception e) {
            log.error("Error processing image: {}", e.getMessage(), e);
            ImageAnalysisResponse errorResponse = ImageAnalysisResponse.builder()
                    .success(false)
                    .message("Error processing image: " + e.getMessage())
                    .build();
            return createResponse(500, errorResponse);
        }
    }

    /**
     * Helper method to create a response
     */
    private APIGatewayProxyResponseEvent createResponse(int statusCode, ImageAnalysisResponse body) {
        try {
            String responseBody = body != null ? objectMapper.writeValueAsString(body) : "";
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withBody(responseBody);
        } catch (Exception e) {
            log.error("Error creating response: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"success\":false,\"message\":\"Internal server error\"}");
        }
    }
}
