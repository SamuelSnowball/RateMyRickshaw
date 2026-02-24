package com.ratemyrickshaw.fn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

/*
Returns a list of detected text that are likely to be the number plate, based on confidence and similarity.

The logic is as follows:
1. Filter for word level detections and exclude common non-number plate words (e.g., "STOP", "KEEP", "DISTANCE").
2. Compare each detected word with every other word to find similar words, e.g the number plate might appear multiple times on a vehicle so pick the clearest one.
3. For similar words, keep the one with the highest confidence score.
4. If no similar words are found, keep all the words.
5. Sort the remaining words by their bounding box position (top to bottom, then left to right) to maintain the order they appear in the image. 
   The number plate text should be close together in the sorted list due to their proximity in the image.
6. Return the sorted list of detected text as potential number plate candidates.
*/
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class NumberPlateExtractionFn implements Function<DetectTextResponse, List<String>> {

    private final SimilarityFn similarityFn;

    private static final List<String> excludedWords = List.of("stop", "keep", "distance");

    @Override
    public List<String> apply(DetectTextResponse rekognitionResponse) {

        // Filter for word level detections
        List<TextDetection> wordDetections = rekognitionResponse.textDetections().stream()
                .filter(textDetection -> "WORD".equals(textDetection.type().toString()))
                .filter(textDetection -> !excludedWords.contains(textDetection.detectedText().toLowerCase()))
                .toList();

        log.info("Detected words: {}", wordDetections.stream().map(TextDetection::detectedText).toList());

        List<TextDetection> highestConfidenceDetections = new ArrayList<>();

        // On^2 loop to compare each text detection with every other.. how to improve?
        for (TextDetection text : wordDetections) {
            if (highestConfidenceDetections.contains(text)) {
                continue; // already added as a similar word
            }

            for (TextDetection otherText : wordDetections) {
                
                if (text.detectedText().equals(otherText.detectedText()))
                    continue; // skip comparing the same detection
                // Otherwise it will obviously evaluate to similar, and get stored in highestConfidenceDetections

                boolean similar = similarityFn.test(text.detectedText(), otherText.detectedText());

                if (similar) {
                    log.info("Treating '{}' and '{}' as the same word", text.detectedText(),
                            otherText.detectedText());
                    // Pick the one with higher confidence
                    TextDetection higherConfidenceDetection = text.confidence() >= otherText.confidence() ? text
                            : otherText;
                    log.info("Keeping '{}' with confidence {}", higherConfidenceDetection.detectedText(),
                            higherConfidenceDetection.confidence());

                    highestConfidenceDetections.add(higherConfidenceDetection);
                } else {
                    log.info("Treating '{}' and '{}' as different words", text.detectedText(),
                            otherText.detectedText());
                }
            }
        }

        if(highestConfidenceDetections.isEmpty()){
            // Then there were no similar words detected, just take all of the words
            highestConfidenceDetections = wordDetections;
        }

        log.info("Highest confidence detections: {}",
                highestConfidenceDetections.stream().map(TextDetection::detectedText).toList());

        // Sort the detected text by bounding region, to get words in order: (top →
        // bottom, left → right)
        // Then concat the words together to form a possible number plate
        return highestConfidenceDetections.stream()
                .sorted(Comparator
                        .comparing((TextDetection td) -> td.geometry().boundingBox().top())
                        .thenComparing(td -> td.geometry().boundingBox().left()))
                .map(text -> {
                    // Debug stage just to log the text
                    log.info("Sorted detected text: {}", text.detectedText());
                    return text;
                })
                .map(TextDetection::detectedText)
                .toList();
    }

}
