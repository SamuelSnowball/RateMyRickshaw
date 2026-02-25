package com.ratemyrickshaw.bean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

/*  
We can't inject something that isn't a Bean, so we need to produce a bean of type JaroWinklerSimilarity which makes
it available for injection.
*/
public class JaroWinklerSimilarityProducer {

    @Produces
    @ApplicationScoped
    public JaroWinklerSimilarity jaroWinklerSimilarity() {
        return new JaroWinklerSimilarity();
    }
}