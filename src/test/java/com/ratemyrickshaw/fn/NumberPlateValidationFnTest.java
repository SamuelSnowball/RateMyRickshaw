package com.ratemyrickshaw.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class NumberPlateValidationFnTest {

    @Inject
    private NumberPlateValidationFn numberPlateValidationFn;

    @Test
    void testValidPlates() {
        String detectedText = "AP13v7951";
        String result = numberPlateValidationFn.apply(detectedText);
        assertEquals("AP13V7951", result);

        detectedText = "DL1RK5954";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("DL1RK5954", result);

        detectedText = "WB39C3870";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("WB39C3870", result);

        detectedText = "MP09R5521";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("MP09R5521", result);

        detectedText = "TN11AP2245";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("TN11AP2245", result);
    }

    @Test
    void testInvalidPlates() {
        String detectedText = "A13v7951";
        String result = numberPlateValidationFn.apply(detectedText);
        assertEquals("Invalid state code", result);

        detectedText = "AP1v7951";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("Invalid RTO code", result);

        detectedText = "AP13V79512";
        result = numberPlateValidationFn.apply(detectedText);
        assertEquals("Invalid number", result);
    }

}