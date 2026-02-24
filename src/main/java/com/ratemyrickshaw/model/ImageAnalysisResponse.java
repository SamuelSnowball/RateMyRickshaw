package com.ratemyrickshaw.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ImageAnalysisResponse {

    String name;
    boolean success;
    String message;
    Float confidence;

    String data;

}
