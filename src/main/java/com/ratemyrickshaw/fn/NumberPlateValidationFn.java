package com.ratemyrickshaw.fn;

import static org.junit.jupiter.api.DynamicTest.stream;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

/*
Validation and normalization.
*/
@ApplicationScoped
public class NumberPlateValidationFn implements Function<List<String>, String> {

    private static final String STATE_REGEX = "^(AP|AR|AS|BR|CH|CG|DD|DL|DN|GA|GJ|HP|HR|JH|JK|KA|KL|LA|LD|MH|ML|MN|MP|MZ|NL|OD|PB|PY|RJ|SK|TN|TR|TS|UK|UP|WB).*$";

    /*
     * RTO can be 1 or 2 digits
     * Delhi uses 1-digit RTO codes (DL1, DL9), others use 2-digit (AP13, MH02).
     */
    private static final String RTO_REGEX = "^[A-Z]{2}\\d{1,2}.*$";

    // Series can be 1–3 letters
    private static final String SERIES_REGEX = "^[A-Z]{2}\\d{1,2}[A-Z]{1,3}.*$";

    // Number is always 4 digits
    private static final String NUMBER_REGEX = "^[A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{4}$";

    @Override
    public String apply(List<String> detectedTexts) {

        String detectedPlate = null;

        // Normalise OCR output
        String normalised = detectedTexts.stream()
                .map(text -> text.toUpperCase().replaceAll("[^A-Z0-9]", "")) // strip noise like spaces, hyphens, etc.
                .reduce("", (a, b) -> a + b);

        if(detectedTexts.size() == 2) {
            // If we only have 2 detections, we can assume we've found the number plate, so just concatenate them
            detectedPlate = detectedTexts.stream().collect(Collectors.joining());
        }
        else {
            // Otherwise if we have more than 2 detections, need to apply some logic to determine which words are most likely to be the number plate
            // For example the detectedTexts at this point could be: [CNG, RECNG, atrone, OLIRE, T.S.R, 5021]
            // The text with the most digits is likely to be the number part of the plate, so we can count the digits in each detected text and pick the one with the most digits
            // And the first part of the number plate should be close (in the array) to the part with the numbers, due to the T->B L->R sorting that was performed.  
            // So we can store the index of the text with the most digits, and then look at the texts before that index to find the other part of the number plate.

            detectedPlate = detectedTexts.stream()
                    .filter(text -> text.chars().filter(Character::isDigit).count() >= 2) // keep texts with at least 2 digits
                    .max(Comparator.comparingLong(text -> text.chars().filter(Character::isDigit).count())) // pick the text with the most digits
                    .map(text -> {
                        // If the text with the most digits has less than 4 digits, we can look for another text that is similar to it and has more digits, and assume they are both part of the number plate
                     
                        }

                        int numberPartIdx = detectedTexts.stream()
                    .filter(text -> text.chars().filter(Character::isDigit).count() >= 2)

                        for(int i = 0; i < detectedTexts.size(); i++) {
                            String otherText = detectedTexts.get(i);
                            if(similarityFn.test(text, otherText) && otherText.chars().filter(Character::isDigit).count() > text.chars().filter(Character::isDigit).count()) {
                                // If we find a similar text with more digits, we can assume they are both part of the number plate, so we concatenate them
                                return otherText + text;
                            }

                        }

        }




   

        boolean validState = normalised.matches(STATE_REGEX);
        boolean validRto = normalised.matches(RTO_REGEX);
        boolean validSeries = normalised.matches(SERIES_REGEX);
        boolean validNumber = normalised.matches(NUMBER_REGEX);

        if (!validState) {
            return "Invalid state code";
        }

        if (!validRto) {
            return "Invalid RTO code";
        }

        if (!validSeries) {
            return "Invalid series code";
        }

        if (!validNumber) {
            return "Invalid number";
        }

        return normalised;
    }

}
