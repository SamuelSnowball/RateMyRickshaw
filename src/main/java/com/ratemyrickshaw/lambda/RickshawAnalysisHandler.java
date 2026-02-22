package com.ratemyrickshaw.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.ratemyrickshaw.model.ImageAnalysisRequest;
import com.ratemyrickshaw.model.ImageAnalysisResponse;
import com.ratemyrickshaw.service.RekognitionService;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("rickshawAnalysis")
public class RickshawAnalysisHandler implements RequestHandler<ImageAnalysisRequest, ImageAnalysisResponse> {

    @Inject
    RekognitionService rekognitionService;

    @Override
    public ImageAnalysisResponse handleRequest(ImageAnalysisRequest request, Context context) {
        context.getLogger().log("Processing image analysis request");
        
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            context.getLogger().log("Analyzing image from URL: " + request.getImageUrl());
            return rekognitionService.analyzeImageFromUrl(request.getImageUrl());
        } else if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
            context.getLogger().log("Analyzing image from base64 data");
            return rekognitionService.analyzeImageFromBase64(request.getImageBase64());
        } else {
            return ImageAnalysisResponse.builder()
                    .success(false)
                    .message("Either imageUrl or imageBase64 must be provided")
                    .build();
        }
    }
}
