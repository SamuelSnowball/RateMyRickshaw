package com.ratemyrickshaw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageAnalysisRequest {
    private String imageUrl;
    private String imageBase64;
}
