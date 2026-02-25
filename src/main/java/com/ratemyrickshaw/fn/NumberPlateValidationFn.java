package com.ratemyrickshaw.fn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/*
Validation and normalization.
*/
@Slf4j
@ApplicationScoped
public class NumberPlateValidationFn implements Function<List<String>, String> {

    private static final String STATE_REGEX = "^(AP|AR|AS|BR|CH|CG|DD|DL|DN|GA|GJ|HP|HR|JH|JK|KA|KL|LA|LD|MH|ML|MN|MP|MZ|NL|OD|PB|PY|RJ|SK|TN|TR|TS|UK|UP|WB)$";

    // Delhi (DL) specific patterns - individual components
    private static final String DL_RTO_REGEX = "^DL\\d$";  // Just DL + 1 digit (e.g., "DL1")
    private static final String DL_COMPLETE_REGEX = "^DL\\d[A-Z]{1,3}\\d{4}$";  // Complete plate

    // Standard patterns - individual components
    private static final String STANDARD_RTO_REGEX = "^[A-Z]{2}\\d{2}$";  // Just state + RTO (e.g., "AP13")
    private static final String STANDARD_SERIES_REGEX = "^[A-Z]{1,3}$";  // Just 1-3 letters (e.g., "AB")
    private static final String STANDARD_COMPLETE_REGEX = "^[A-Z]{2}\\d{2}[A-Z]{1,3}\\d{4}$";  // Complete plate

    @Override
    public String apply(List<String> detectedTexts) {

        // Normalize OCR output: uppercase, strip non-alphanumeric, and correct common OCR errors
        detectedTexts = detectedTexts.stream()
                .map(String::toUpperCase)
                .map(text -> text.replaceAll("[^A-Z0-9]", "")) // Remove periods, hyphens, spaces, etc.
                .filter(text -> !text.isEmpty()) // Remove empty strings after cleanup
                .map(this::correctOcrErrors)
                .map(this::correctCharToDigitErrors)
                .toList();

        return assembleNumberPlate(detectedTexts);
    }

    /*
     * If we only have 2 detections, we can assume we've found the number plate, so just concatenate them.
     * Otherwise if we have more than 2 detections, need to apply some logic to
     * determine which words are most likely to be the number plate
     * For example the detectedTexts at this point could be: [CNG, RECNG, atrone,
     * OLIRE, T.S.R, 5021]
     * The text with the most digits is likely to be the number part of the plate,
     * so we can count the digits in each detected text and pick the one with the
     * most digits
     * And we can see if other word matches pass the regexes for state code, RTO
     * code, or series code, and if they do, we can assume they're part of the
     * number plate as well.
     */
    private String assembleNumberPlate(List<String> detectedTexts){
        
        String detectedPlate = null;    

        if (detectedTexts.isEmpty()) {
                return "No text detected";
        }

        if (detectedTexts.size() == 1) {
            detectedPlate = detectedTexts.get(0);
        } else if (detectedTexts.size() == 2) {
            detectedPlate = detectedTexts.stream().collect(Collectors.joining());
        } else {
            int numberPartIdx = detectedTexts.stream()
                    .max(Comparator.comparingInt(s -> (int) s.chars()
                            .filter(Character::isDigit)
                            .count()))
                    .map(detectedTexts::indexOf)
                    .orElse(-1);

            List<String> matches = new ArrayList<>();

            // For every text detection, see how many regexes it matches (state code, RTO code, series code), 
            // and if it matches enough regexes based on its length, then we can be reasonably sure it's part of the number plate 
            // and add it to the matches list.
            for (int i = 0; i < numberPartIdx; i++) {
                String otherText = detectedTexts.get(i);
                int matchCount = 0;
                StringBuilder matchedRegexes = new StringBuilder("[");
                
                // Check state code (first 2 chars)
                if (otherText.length() >= 2 && otherText.substring(0, 2).matches(STATE_REGEX)) {
                    matchCount++;
                    matchedRegexes.append("STATE,");
                }
                
                // Check RTO code based on state
                if (otherText.length() >= 3) {
                    String statePrefix = otherText.substring(0, 2);
                    if (statePrefix.equals("DL")) {
                        // Delhi: check 3 chars (DL + 1 digit)
                        if (otherText.substring(0, 3).matches(DL_RTO_REGEX)) {
                            matchCount++;
                            matchedRegexes.append("DL_RTO,");
                        }
                    } else if (otherText.length() >= 4) {
                        // Standard: check 4 chars (state + 2 digits)
                        if (otherText.substring(0, 4).matches(STANDARD_RTO_REGEX)) {
                            matchCount++;
                            matchedRegexes.append("STANDARD_RTO,");
                        }
                    }
                }
                
                // Check series code (extract middle portion)
                if (otherText.length() >= 4) {
                    String statePrefix = otherText.substring(0, 2);
                    if (statePrefix.equals("DL") && otherText.length() >= 5) {
                        // Delhi: series starts at position 3 (after DL1)
                        String series = otherText.substring(3, Math.min(6, otherText.length()));
                        // Extract just letters before any digits
                        String seriesLetters = series.replaceAll("\\d.*$", "");
                        if (!seriesLetters.isEmpty() && seriesLetters.matches(STANDARD_SERIES_REGEX)) {
                            matchCount++;
                            matchedRegexes.append("DL_SERIES,");
                        }
                    } else if (otherText.length() >= 5) {
                        // Standard: series starts at position 4 (after state + 2 digits)
                        String series = otherText.substring(4, Math.min(7, otherText.length()));
                        // Extract just letters before any digits
                        String seriesLetters = series.replaceAll("\\d.*$", "");
                        if (!seriesLetters.isEmpty() && seriesLetters.matches(STANDARD_SERIES_REGEX)) {
                            matchCount++;
                            matchedRegexes.append("STANDARD_SERIES,");
                        }
                    }
                }
                
                // Validate based on word length
                boolean isValid = false;
                if (otherText.length() == 2) {
                    // Should match state OR be 2 digits
                    isValid = matchCount == 1 || otherText.matches("^\\d{2}$");
                } else if (otherText.length() == 3) {
                    // Should match state + RTO (Delhi) = 2 matches
                    isValid = matchCount == 2;
                } else if (otherText.length() == 4) {
                    // Should match state + RTO (standard) = 2 matches
                    isValid = matchCount == 2;
                } else if (otherText.length() >= 5) {
                    // Should match state + RTO + series = 3 matches
                    isValid = matchCount == 3;
                } else {
                    // Handle longer words?
                }
                
                // Log the regex matches
                String regexList = matchedRegexes.length() > 1 
                    ? matchedRegexes.substring(0, matchedRegexes.length() - 1) + "]" 
                    : "[]";
                log.info("Word: '{}' (length: {}) - Matched regexes: {} - Match count: {} - Valid: {}", 
                    otherText, otherText.length(), regexList, matchCount, isValid);
                    
                if (isValid) {
                    matches.add(otherText);
                }
            }

            if (!matches.isEmpty()) {
                detectedPlate = String.join("", matches) + detectedTexts.get(numberPartIdx);
            }
        }

        Objects.requireNonNull(detectedPlate, "No valid number plate detected");

        // Use Delhi-specific or standard regex based on state code
        String stateCode = detectedPlate.substring(0, 2);
        boolean isDehli = stateCode.equals("DL");
        
        // Validate complete plate format
        boolean validNumber = isDehli 
            ? detectedPlate.matches(DL_COMPLETE_REGEX) 
            : detectedPlate.matches(STANDARD_COMPLETE_REGEX);

        if (!validNumber) {
            // Try to provide more specific error message
            // Check state code (first 2 chars)
            if (detectedPlate.length() < 2 || !detectedPlate.substring(0, 2).matches(STATE_REGEX)) {
                return "Invalid state code";
            }
            // Extract and validate RTO component
            int rtoLength = isDehli ? 3 : 4;
            if (detectedPlate.length() < rtoLength) {
                return "Invalid RTO code";
            }
            String rto = detectedPlate.substring(0, rtoLength);
            if (!rto.matches(isDehli ? DL_RTO_REGEX : STANDARD_RTO_REGEX)) {
                return "Invalid RTO code";
            }
            return "Invalid plate format";
        }
        return detectedPlate;
    }

    
    /**
     * Corrects common OCR misreadings of state codes
     * 
     * @param text The text to correct
     * @return Corrected text
     */
    private String correctOcrErrors(String text) {
        if (text.length() < 2) {
            return text;
        }

        String firstTwo = text.substring(0, 2);
        String rest = text.substring(2);

        // Common OCR mistakes for Indian state codes
        String corrected = switch (firstTwo) {
            case "OL" -> "DL"; // O confused with D (Delhi)
            case "0L" -> "DL"; // 0 confused with D (Delhi)
            case "D1" -> "DL"; // 1 confused with L (Delhi)
            case "0D" -> "OD"; // 0 confused with O (Odisha)
            case "QB" -> "PB"; // Q confused with P (Punjab)
            case "P8" -> "PB"; // 8 confused with B (Punjab)
            case "K1" -> "KL"; // 1 confused with L (Kerala)
            case "M4" -> "MH"; // 4 confused with H (Maharashtra)
            default -> {
                // 1 confused with L (Kerala) - but KL is correct
                if (firstTwo.equals("KL") && text.startsWith("K1"))
                    yield "KL";
                // 4 confused with H (Maharashtra) - but MH is correct
                if (firstTwo.equals("MH") && text.startsWith("M4"))
                    yield "MH";
                yield firstTwo;
            }
        };

        return corrected + rest;
    }

    /**
     * Corrects common OCR misreadings where characters are mistaken for digits
     * in RTO codes (position 3-4 after state code, or position 3 for Delhi)
     * 
     * Delhi (DL) uses 1-digit RTO codes, all other states use 2-digit codes.
     * 
     * @param text The text to correct
     * @return Corrected text with digit substitutions
     */
    private String correctCharToDigitErrors(String text) {
        if (text.length() < 3) {
            return text;
        }

        String stateCode = text.substring(0, 2);
        boolean isDehli = stateCode.equals("DL");
        
        // For Delhi: 1-digit RTO (position 3), for others: 2-digit RTO (positions 3-4)
        int rtoLength = isDehli ? 1 : 2;
        int rtoEndIndex = Math.min(2 + rtoLength, text.length());
        
        String rtoCode = text.substring(2, rtoEndIndex);
        String rest = text.length() > rtoEndIndex ? text.substring(rtoEndIndex) : "";

        // Correct common character-to-digit OCR mistakes in RTO code
        String correctedRto = rtoCode
                .replace('O', '0')  // O → 0
                .replace('o', '0')  // o → 0
                .replace('I', '1')  // I → 1
                .replace('L', '1')  // L (uppercase L) → 1
                .replace('l', '1')  // l (lowercase L) → 1
                .replace('Z', '2')  // Z → 2
                .replace('z', '2')  // z → 2
                .replace('S', '5')  // S → 5
                .replace('s', '5')  // s → 5
                .replace('G', '6')  // G → 6
                .replace('g', '9')  // g → 9
                .replace('T', '7')  // T → 7
                .replace('B', '8')  // B → 8
                .replace('D', '0')  // D → 0
                .replace('A', '4')  // A → 4
                .replace('E', '3')  // E → 3
                .replace('R', '8'); // R → 8

        return stateCode + correctedRto + rest;
    }

}
