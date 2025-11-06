package com.slipstream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of anomaly detection analysis.
 * This is sent to output topics when anomalies are detected.
 */
public class AnomalyResult {
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("is_anomaly")
    private boolean isAnomaly;
    
    @JsonProperty("anomaly_score")
    private double anomalyScore;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("anomaly_type")
    private AnomalyType anomalyType;
    
    @JsonProperty("detected_at")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime detectedAt;
    
    @JsonProperty("original_transaction")
    private TransactionEvent originalTransaction;
    
    @JsonProperty("features_used")
    private Map<String, Double> featuresUsed;
    
    @JsonProperty("reason")
    private String reason;

    public AnomalyResult() {}

    public AnomalyResult(String transactionId, boolean isAnomaly, double anomalyScore, 
                        double confidence, AnomalyType anomalyType, LocalDateTime detectedAt,
                        TransactionEvent originalTransaction, Map<String, Double> featuresUsed, 
                        String reason) {
        this.transactionId = transactionId;
        this.isAnomaly = isAnomaly;
        this.anomalyScore = anomalyScore;
        this.confidence = confidence;
        this.anomalyType = anomalyType;
        this.detectedAt = detectedAt;
        this.originalTransaction = originalTransaction;
        this.featuresUsed = featuresUsed;
        this.reason = reason;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public AnomalyType getAnomalyType() { return anomalyType; }
    public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public TransactionEvent getOriginalTransaction() { return originalTransaction; }
    public void setOriginalTransaction(TransactionEvent originalTransaction) { this.originalTransaction = originalTransaction; }

    public Map<String, Double> getFeaturesUsed() { return featuresUsed; }
    public void setFeaturesUsed(Map<String, Double> featuresUsed) { this.featuresUsed = featuresUsed; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnomalyResult that = (AnomalyResult) o;
        return isAnomaly == that.isAnomaly &&
                Double.compare(that.anomalyScore, anomalyScore) == 0 &&
                Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(transactionId, that.transactionId) &&
                anomalyType == that.anomalyType &&
                Objects.equals(detectedAt, that.detectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, isAnomaly, anomalyScore, confidence, 
                          anomalyType, detectedAt);
    }

    @Override
    public String toString() {
        return "AnomalyResult{" +
                "transactionId='" + transactionId + '\'' +
                ", isAnomaly=" + isAnomaly +
                ", anomalyScore=" + anomalyScore +
                ", confidence=" + confidence +
                ", anomalyType=" + anomalyType +
                ", detectedAt=" + detectedAt +
                ", reason='" + reason + '\'' +
                '}';
    }

    /**
     * Enum defining different types of anomalies that can be detected
     */
    public enum AnomalyType {
        @JsonProperty("fraud")
        FRAUD("fraud"),
        
        @JsonProperty("unusual_amount")
        UNUSUAL_AMOUNT("unusual_amount"),
        
        @JsonProperty("velocity")
        VELOCITY("velocity"),
        
        @JsonProperty("location")
        LOCATION("location"),
        
        @JsonProperty("time_pattern")
        TIME_PATTERN("time_pattern"),
        
        @JsonProperty("merchant_pattern")
        MERCHANT_PATTERN("merchant_pattern"),
        
        @JsonProperty("statistical_outlier")
        STATISTICAL_OUTLIER("statistical_outlier"),
        
        @JsonProperty("unknown")
        UNKNOWN("unknown");

        private final String value;

        AnomalyType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}