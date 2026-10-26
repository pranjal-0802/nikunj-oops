package com.iitsm.infrastructure.config;

import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.domain.strategy.assignment.CandidateFilter;
import com.iitsm.domain.strategy.classification.CompositeClassifier;
import com.iitsm.domain.strategy.classification.KeywordClassifier;
import com.iitsm.domain.strategy.classification.NaiveBayesClassifier;
import com.iitsm.domain.strategy.classification.ServiceMatcher;
import com.iitsm.domain.strategy.nlp.TfIdfVectorizer;
import com.iitsm.domain.strategy.priority.CompositePriorityCalculator;
import com.iitsm.domain.strategy.similarity.CosineTfIdfSimilarity;
import com.iitsm.domain.strategy.similarity.SimilarityMeasure;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring configuration initializing intelligence subsystem components (NLP, ML classifiers, priority engine, assignment strategies).
 */
@Configuration
public class IntelligenceConfig {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceConfig.class);
    private static final String CORPUS_PATH = "sim/corpus.csv";

    @Bean
    public TfIdfVectorizer tfIdfVectorizer() {
        TfIdfVectorizer vectorizer = new TfIdfVectorizer();
        List<String> documents = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource(CORPUS_PATH);
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean header = true;
                    while ((line = reader.readLine()) != null) {
                        if (header) {
                            header = false;
                            continue;
                        }
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 2) {
                            documents.add((parts.length == 3) ? parts[1] + " " + parts[2] : parts[1]);
                        }
                    }
                }
                vectorizer.train(documents);
                log.info("Trained TfIdfVectorizer on {} corpus documents with vocabulary size {}",
                        documents.size(), vectorizer.getVocabularySize());
            } else {
                log.warn("Corpus file {} not found; TfIdfVectorizer initialized without baseline corpus", CORPUS_PATH);
            }
        } catch (Exception ex) {
            log.error("Failed to train TfIdfVectorizer from corpus: {}", ex.getMessage(), ex);
        }

        return vectorizer;
    }

    @Bean
    public KeywordClassifier keywordClassifier() {
        return new KeywordClassifier();
    }

    @Bean
    public NaiveBayesClassifier naiveBayesClassifier() {
        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        try {
            ClassPathResource resource = new ClassPathResource(CORPUS_PATH);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    classifier.trainFromCsv(is);
                    log.info("Trained NaiveBayesClassifier on corpus with vocabulary size {}",
                            classifier.getVocabularySize());
                }
            } else {
                log.warn("Corpus file {} not found; NaiveBayesClassifier initialized untrained", CORPUS_PATH);
            }
        } catch (Exception ex) {
            log.error("Failed to train NaiveBayesClassifier from corpus: {}", ex.getMessage(), ex);
        }
        return classifier;
    }

    @Bean
    @Primary
    public CompositeClassifier compositeClassifier(
            KeywordClassifier keywordClassifier,
            NaiveBayesClassifier naiveBayesClassifier) {
        return new CompositeClassifier(keywordClassifier, naiveBayesClassifier);
    }

    @Bean
    public ServiceMatcher serviceMatcher() {
        return new ServiceMatcher();
    }

    @Bean
    public CompositePriorityCalculator compositePriorityCalculator() {
        return new CompositePriorityCalculator();
    }

    @Bean
    public CandidateFilter candidateFilter() {
        return new CandidateFilter();
    }

    @Bean
    public AssignmentStrategyRegistry assignmentStrategyRegistry() {
        return new AssignmentStrategyRegistry();
    }

    @Bean
    public SimilarityMeasure similarityMeasure(TfIdfVectorizer vectorizer) {
        return new CosineTfIdfSimilarity(vectorizer);
    }
}

