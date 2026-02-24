package com.ratemyrickshaw.service;

import java.util.Optional;

import com.ratemyrickshaw.fn.NumberPlateExtractionFn;
import com.ratemyrickshaw.fn.NumberPlateValidationFn;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PostRekognitionService {

    private final NumberPlateExtractionFn numberPlateExtractionFn;
    private final NumberPlateValidationFn numberPlateValidationFn;

    public String postProcessTextDetections(DetectTextResponse rekognitionResponse) {

        return Optional.of(numberPlateExtractionFn.apply(rekognitionResponse))
                .map(numberPlateValidationFn) 
                .orElse("Invalid number plate");
    }

}
