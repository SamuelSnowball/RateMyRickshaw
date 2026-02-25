package com.ratemyrickshaw.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class NumberPlateValidationFnTest {

    @Inject
    private NumberPlateValidationFn numberPlateValidationFn;

    @Test
    void testValidPlates() {
        List<String> detections = List.of("AP13v", "7951");
        String result = numberPlateValidationFn.apply(detections);
        assertEquals("AP13V7951", result);

        detections = List.of("AP13", "T.S.R", "v7951");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("AP13V7951", result);

        detections = List.of("DL1R", "K5954");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("DL1RK5954", result);

        detections = List.of("WB39", "C3870");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("WB39C3870", result);

        detections = List.of("MP09", "R5521");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("MP09R5521", result);

        detections = List.of("TN11", "AP", "2245");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("TN11AP2245", result);

        detections = List.of("TN11", "AP2245");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("TN11AP2245", result);

        // Test with noise in the detections
        // There's no OL state, it should be DL
        // And the IRE should actually be 1RE
        detections = List.of("CNG", "RECNG", "atrone", "OLIRE", "T.S.R", "5021");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("DL1RE5021", result);

        // TNll should be recognized as numbers as its part of the RTO code
        detections = List.of("CNG", "RECNG", "atrone", "TNllAP", "T.S.R", "5021");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("TN11AP5021", result);

        // MH03 should be chosen over MF50 as MH is a valid state but MF is not
        // Number part should be last
        detections = List.of("MF50", "MH03", "V5823");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("MH03V5823", result);
    }

    @Test
    void testInvalidPlates() {
        // Invalid state code - single letter
        List<String> detections = List.of("A13v", "7951");
        String result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid state code", result);

        // Invalid state code - non-existent state
        detections = List.of("ZZ13", "7951");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid state code", result);

        // Invalid RTO code - missing digits (concatenates to "AP7951" which becomes AP + 79 + 51)
        // This actually forms AP79 (valid RTO pattern) + 51 (invalid number), so it's invalid plate format
        detections = List.of("AP", "7951");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);

        // Invalid RTO code - Delhi with 2 digits instead of 1 (becomes DL12 + 5954 = DL125954)
        // DL1 is valid RTO, but then 25954 doesn't fit series+number pattern
        detections = List.of("DL12", "5954");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);

        // Missing series letters
        detections = List.of("AP13", "7951");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);

        // Wrong number of digits (3 instead of 4)
        detections = List.of("AP13V", "795");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);

        // Series with too many letters (4 instead of max 3)
        detections = List.of("AP13VWXY", "7951");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);

        // Missing number part entirely
        detections = List.of("AP13V");
        result = numberPlateValidationFn.apply(detections);
        assertEquals("Invalid plate format", result);
    }

}