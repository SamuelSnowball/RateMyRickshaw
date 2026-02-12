package com.ratemyrickshaw.model;

import java.util.List;
import java.util.Map;

public class ImageAnalysisResponse {
    private boolean success;
    private String message;
    private List<String> labels;
    private Map<String, Float> labelConfidence;
    private boolean isRickshaw;
    private float rickshawConfidence;

    public ImageAnalysisResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Map<String, Float> getLabelConfidence() {
        return labelConfidence;
    }

    public void setLabelConfidence(Map<String, Float> labelConfidence) {
        this.labelConfidence = labelConfidence;
    }

    public boolean isRickshaw() {
        return isRickshaw;
    }

    public void setRickshaw(boolean rickshaw) {
        isRickshaw = rickshaw;
    }

    public float getRickshawConfidence() {
        return rickshawConfidence;
    }

    public void setRickshawConfidence(float rickshawConfidence) {
        this.rickshawConfidence = rickshawConfidence;
    }
}
