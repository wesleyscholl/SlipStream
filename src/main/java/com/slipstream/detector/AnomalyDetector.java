package com.slipstream.detector;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;

/**
 * Interface for anomaly detection algorithms.
 * Implementations should be thread-safe as they will be used in Kafka Streams.
 */
public interface AnomalyDetector {
    
    /**
     * Analyzes a transaction event and determines if it's anomalous
     * 
     * @param transaction The transaction to analyze
     * @return AnomalyResult containing detection results
     */
    AnomalyResult detect(TransactionEvent transaction);
    
    /**
     * Updates the detector's model with a new transaction
     * This is used for online learning and adaptive models
     * 
     * @param transaction The transaction to learn from
     */
    void updateModel(TransactionEvent transaction);
    
    /**
     * Returns the name/type of this detector
     */
    String getDetectorName();
    
    /**
     * Returns whether this detector supports online learning
     */
    boolean supportsOnlineLearning();
}