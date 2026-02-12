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
        
        if (request.getS3Bucket() != null && request.getS3Key() != null) {
            context.getLogger().log("Analyzing image from S3: " + request.getS3Bucket() + "/" + request.getS3Key());
            return rekognitionService.analyzeImage(request.getS3Bucket(), request.getS3Key());
        } else if (request.getImageUrl() != null) {
            context.getLogger().log("Analyzing image from URL: " + request.getImageUrl());
            return rekognitionService.analyzeImageFromUrl(request.getImageUrl());
        } else {
            ImageAnalysisResponse response = new ImageAnalysisResponse();
            response.setSuccess(false);
            response.setMessage("Either s3Bucket/s3Key or imageUrl must be provided");
            return response;
        }
    }
}
