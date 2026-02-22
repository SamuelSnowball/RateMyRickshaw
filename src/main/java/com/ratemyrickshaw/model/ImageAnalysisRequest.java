package com.ratemyrickshaw.model;

public class ImageAnalysisRequest {
    private String imageUrl;
    private String imageBase64;

    public ImageAnalysisRequest() {
    }

    public ImageAnalysisRequest(String imageUrl, String imageBase64) {
        this.imageUrl = imageUrl;
        this.imageBase64 = imageBase64;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
