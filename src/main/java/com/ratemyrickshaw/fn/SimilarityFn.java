package com.ratemyrickshaw.fn;

import java.util.function.BiPredicate;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class SimilarityFn implements BiPredicate<String, String> {

    private final JaroWinklerSimilarity jw;
    private static final double SIMILARITY_THRESHOLD = 0.65;

    @Override
    public boolean test(String detectedText1, String detectedText2) {
        if (detectedText1 == null || detectedText2 == null) {
            return false;
        }
        detectedText1 = detectedText1.trim();
        detectedText2 = detectedText2.trim();

        // 1. Reject strings with significant length differences
        if (Math.abs(detectedText1.length() - detectedText2.length()) > 2) {
            return false;
        }

        // 2. Compute Levenshtein distance, if distance is too large, reject early
        int lev = levenshtein(detectedText1, detectedText2);
        if (lev > 3) {
            return false;
        }

        // At this point we can be reasonably sure the strings are similar enough to compare with Jaro–Winkler.
        // So if we get a high confidence we can be pretty sure it's a good match.

        // 3. Use Jaro–Winkler for fine similarity scoring
        double confidence = jw.apply(detectedText1, detectedText2);
        
        return confidence > SIMILARITY_THRESHOLD;
    }

    // Basic Levenshtein implementation
    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++)
            dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }

}
