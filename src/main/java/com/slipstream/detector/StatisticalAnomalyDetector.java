package com.slipstream.detector;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Statistical anomaly detector using Z-score and rule-based detection.
 * This detector is particularly effective for fraud detection using statistical methods.
 */
public class StatisticalAnomalyDetector implements AnomalyDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticalAnomalyDetector.class);
    
    private static final double DEFAULT_Z_SCORE_THRESHOLD = 2.5;
    private static final double DEFAULT_ANOMALY_THRESHOLD = 0.7;
    private static final int MIN_TRAINING_SAMPLES = 20;
    private static final long VELOCITY_WINDOW_MS = 60000; // 1 minute
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 10;
    
    private final DescriptiveStatistics globalAmountStats;
    private final DescriptiveStatistics globalTimeStats;
    private final Map<String, DescriptiveStatistics> userAmountStats;
    private final Map<String, Integer> userTransactionCounts;
    private final Map<String, Long> userLastTransactionTime;
    private final Map<String, Integer> userVelocityCount;
    private final AtomicInteger totalTransactions;
    
    private final double zScoreThreshold;
    private final double anomalyThreshold;
    private volatile boolean modelTrained;

    public StatisticalAnomalyDetector() {
        this(DEFAULT_Z_SCORE_THRESHOLD, DEFAULT_ANOMALY_THRESHOLD);
    }

    public StatisticalAnomalyDetector(double zScoreThreshold, double anomalyThreshold) {
        this.zScoreThreshold = zScoreThreshold;
        this.anomalyThreshold = anomalyThreshold;
        this.globalAmountStats = new DescriptiveStatistics();
        this.globalTimeStats = new DescriptiveStatistics();
        this.userAmountStats = new ConcurrentHashMap<>();
        this.userTransactionCounts = new ConcurrentHashMap<>();
        this.userLastTransactionTime = new ConcurrentHashMap<>();
        this.userVelocityCount = new ConcurrentHashMap<>();
        this.totalTransactions = new AtomicInteger(0);
        this.modelTrained = false;
        
        // Keep a sliding window of 1000 samples for global stats
        globalAmountStats.setWindowSize(1000);
        globalTimeStats.setWindowSize(1000);
        
        logger.info("Initialized Statistical Anomaly Detector with Z-score threshold {}, anomaly threshold {}",
                   zScoreThreshold, anomalyThreshold);
    }

    @Override
    public AnomalyResult detect(TransactionEvent transaction) {
        if (!modelTrained && totalTransactions.get() < MIN_TRAINING_SAMPLES) {
            // Not enough data to make reliable predictions
            return performRuleBasedDetection(transaction);
        }

        try {
            double compositeScore = calculateCompositeAnomalyScore(transaction);
            boolean isAnomaly = compositeScore > anomalyThreshold;
            
            AnomalyResult.AnomalyType anomalyType = determineAnomalyType(transaction, compositeScore);
            String reason = generateReason(transaction, compositeScore, anomalyType);
            
            Map<String, Double> features = extractFeatureMap(transaction);
            
            return new AnomalyResult(
                transaction.getTransactionId(),
                isAnomaly,
                compositeScore,
                calculateConfidence(compositeScore),
                anomalyType,
                LocalDateTime.now(),
                transaction,
                features,
                reason
            );
            
        } catch (Exception e) {
            logger.error("Error during anomaly detection for transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
            return createErrorResult(transaction, "Detection error: " + e.getMessage());
        }
    }

    @Override
    public void updateModel(TransactionEvent transaction) {
        try {
            synchronized (this) {
                // Update global statistics
                globalAmountStats.addValue(transaction.getAmount());
                
                // Convert timestamp to hour of day for time patterns
                int hourOfDay = transaction.getTimestamp().getHour();
                globalTimeStats.addValue(hourOfDay);
                
                // Update user-specific statistics
                updateUserStatistics(transaction);
                
                totalTransactions.incrementAndGet();
                
                // Mark model as trained after sufficient samples
                if (!modelTrained && totalTransactions.get() >= MIN_TRAINING_SAMPLES) {
                    modelTrained = true;
                    logger.info("Statistical model is now trained with {} samples", totalTransactions.get());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error updating model with transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Calculates a composite anomaly score based on multiple features
     */
    private double calculateCompositeAnomalyScore(TransactionEvent transaction) {
        double amountScore = calculateAmountAnomalyScore(transaction);
        double timeScore = calculateTimeAnomalyScore(transaction);
        double velocityScore = calculateVelocityAnomalyScore(transaction);
        double locationScore = calculateLocationAnomalyScore(transaction);
        
        // Weighted composite score
        double compositeScore = 0.4 * amountScore + 0.2 * timeScore + 0.3 * velocityScore + 0.1 * locationScore;
        
        return Math.min(compositeScore, 1.0); // Cap at 1.0
    }

    /**
     * Calculates anomaly score based on transaction amount
     */
    private double calculateAmountAnomalyScore(TransactionEvent transaction) {
        double amount = transaction.getAmount();
        
        // Global amount Z-score
        double globalZScore = 0.0;
        if (globalAmountStats.getN() > 1) {
            double mean = globalAmountStats.getMean();
            double std = globalAmountStats.getStandardDeviation();
            if (std > 0) {
                globalZScore = Math.abs((amount - mean) / std);
            }
        }
        
        // User-specific amount analysis
        String userId = transaction.getUserId();
        double userZScore = 0.0;
        DescriptiveStatistics userStats = userAmountStats.get(userId);
        if (userStats != null && userStats.getN() > 1) {
            double userMean = userStats.getMean();
            double userStd = userStats.getStandardDeviation();
            if (userStd > 0) {
                userZScore = Math.abs((amount - userMean) / userStd);
            }
        }
        
        // Rule-based checks
        double ruleScore = 0.0;
        if (amount > 10000) ruleScore += 0.5; // Large amount
        if (amount > 50000) ruleScore += 0.3; // Very large amount
        
        // Combine scores
        double maxZScore = Math.max(globalZScore, userZScore);
        double zScoreNormalized = Math.min(maxZScore / zScoreThreshold, 1.0);
        
        return Math.max(zScoreNormalized, ruleScore);
    }

    /**
     * Calculates anomaly score based on transaction time
     */
    private double calculateTimeAnomalyScore(TransactionEvent transaction) {
        int hour = transaction.getTimestamp().getHour();
        
        // Rule-based time analysis
        double timeScore = 0.0;
        if (hour < 6 || hour > 22) {
            timeScore += 0.6; // Unusual hours
        }
        if (hour < 3 || (hour > 23)) {
            timeScore += 0.3; // Very unusual hours
        }
        
        // Statistical time analysis
        if (globalTimeStats.getN() > MIN_TRAINING_SAMPLES) {
            double timeMean = globalTimeStats.getMean();
            double timeStd = globalTimeStats.getStandardDeviation();
            if (timeStd > 0) {
                double timeZScore = Math.abs((hour - timeMean) / timeStd);
                double timeZScoreNormalized = Math.min(timeZScore / zScoreThreshold, 1.0);
                timeScore = Math.max(timeScore, timeZScoreNormalized * 0.5);
            }
        }
        
        return Math.min(timeScore, 1.0);
    }

    /**
     * Calculates anomaly score based on transaction velocity
     */
    private double calculateVelocityAnomalyScore(TransactionEvent transaction) {
        String userId = transaction.getUserId();
        long currentTime = System.currentTimeMillis();
        
        // Check velocity (transactions per minute)
        Long lastTransactionTime = userLastTransactionTime.get(userId);
        if (lastTransactionTime != null) {
            long timeDiff = currentTime - lastTransactionTime;
            if (timeDiff < VELOCITY_WINDOW_MS) {
                int velocityCount = userVelocityCount.getOrDefault(userId, 0) + 1;
                userVelocityCount.put(userId, velocityCount);
                
                if (velocityCount > MAX_TRANSACTIONS_PER_MINUTE) {
                    return 0.8; // High velocity anomaly
                } else if (velocityCount > MAX_TRANSACTIONS_PER_MINUTE / 2) {
                    return 0.4; // Medium velocity
                }
            } else {
                // Reset velocity counter if outside window
                userVelocityCount.put(userId, 1);
            }
        } else {
            userVelocityCount.put(userId, 1);
        }
        
        userLastTransactionTime.put(userId, currentTime);
        return 0.0;
    }

    /**
     * Calculates anomaly score based on location (simple implementation)
     */
    private double calculateLocationAnomalyScore(TransactionEvent transaction) {
        // Simple location-based scoring
        // In a real system, this would be much more sophisticated
        
        if (transaction.getLocation() == null) {
            return 0.1; // Missing location is slightly suspicious
        }
        
        // For now, just check for very unusual coordinates
        double lat = transaction.getLocation().getLatitude();
        double lon = transaction.getLocation().getLongitude();
        
        // Very rough bounds check (this is overly simplistic)
        if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            return 0.8; // Invalid coordinates
        }
        
        return 0.0;
    }

    /**
     * Updates user-specific statistics
     */
    private void updateUserStatistics(TransactionEvent transaction) {
        String userId = transaction.getUserId();
        double amount = transaction.getAmount();
        
        // Update user amount statistics
        userAmountStats.computeIfAbsent(userId, k -> {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            stats.setWindowSize(100); // Keep last 100 transactions per user
            return stats;
        }).addValue(amount);
        
        // Update transaction count
        userTransactionCounts.merge(userId, 1, Integer::sum);
    }

    /**
     * Determines the primary type of anomaly
     */
    private AnomalyResult.AnomalyType determineAnomalyType(TransactionEvent transaction, double score) {
        double amountScore = calculateAmountAnomalyScore(transaction);
        double timeScore = calculateTimeAnomalyScore(transaction);
        double velocityScore = calculateVelocityAnomalyScore(transaction);
        
        // Return the type with highest contribution
        if (velocityScore > 0.5) {
            return AnomalyResult.AnomalyType.VELOCITY;
        } else if (amountScore > 0.6) {
            return AnomalyResult.AnomalyType.UNUSUAL_AMOUNT;
        } else if (timeScore > 0.5) {
            return AnomalyResult.AnomalyType.TIME_PATTERN;
        } else if (transaction.getAmount() > 10000) {
            return AnomalyResult.AnomalyType.FRAUD;
        } else {
            return AnomalyResult.AnomalyType.STATISTICAL_OUTLIER;
        }
    }

    /**
     * Generates a human-readable reason for the detection result
     */
    private String generateReason(TransactionEvent transaction, double score, AnomalyResult.AnomalyType type) {
        StringBuilder reason = new StringBuilder();
        reason.append("Composite anomaly score: ").append(String.format("%.3f", score));
        reason.append(", Type: ").append(type.getValue());
        
        switch (type) {
            case UNUSUAL_AMOUNT:
                reason.append(", Amount: $").append(transaction.getAmount());
                break;
            case TIME_PATTERN:
                reason.append(", Time: ").append(transaction.getTimestamp().getHour()).append(":00");
                break;
            case VELOCITY:
                reason.append(", High transaction frequency");
                break;
            case FRAUD:
                reason.append(", Potential fraud indicators");
                break;
            default:
                reason.append(", Statistical outlier detected");
        }
        
        return reason.toString();
    }

    /**
     * Rule-based detection for when statistical model is not yet available
     */
    private AnomalyResult performRuleBasedDetection(TransactionEvent transaction) {
        boolean isAnomaly = false;
        AnomalyResult.AnomalyType type = AnomalyResult.AnomalyType.UNKNOWN;
        double score = 0.0;
        String reason = "Rule-based detection: ";
        
        // Large amount rule
        if (transaction.getAmount() > 5000) {
            isAnomaly = true;
            type = AnomalyResult.AnomalyType.UNUSUAL_AMOUNT;
            score = 0.8;
            reason += "Large amount";
        }
        
        // Time-based rule
        int hour = transaction.getTimestamp().getHour();
        if (hour < 6 || hour > 22) {
            isAnomaly = true;
            type = AnomalyResult.AnomalyType.TIME_PATTERN;
            score = Math.max(score, 0.7);
            reason += (score == 0.7 ? "" : ", ") + "Unusual time";
        }
        
        if (!isAnomaly) {
            reason = "Normal transaction";
        }
        
        Map<String, Double> features = extractFeatureMap(transaction);
        
        return new AnomalyResult(
            transaction.getTransactionId(),
            isAnomaly,
            score,
            isAnomaly ? 0.6 : 0.9, // Lower confidence for rule-based
            type,
            LocalDateTime.now(),
            transaction,
            features,
            reason
        );
    }

    private AnomalyResult createErrorResult(TransactionEvent transaction, String reason) {
        return new AnomalyResult(
            transaction.getTransactionId(),
            false,
            0.0,
            0.1,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.now(),
            transaction,
            new HashMap<>(),
            reason
        );
    }

    private double calculateConfidence(double anomalyScore) {
        // Higher scores = higher confidence when anomaly
        // Lower scores = higher confidence when normal
        if (anomalyScore > anomalyThreshold) {
            return 0.5 + (anomalyScore * 0.4); // 0.5 to 0.9
        } else {
            return 0.9 - (anomalyScore * 0.3); // 0.9 to 0.6
        }
    }

    private Map<String, Double> extractFeatureMap(TransactionEvent transaction) {
        Map<String, Double> features = new HashMap<>();
        features.put("amount", transaction.getAmount());
        features.put("hour_of_day", (double) transaction.getTimestamp().getHour());
        features.put("day_of_week", (double) transaction.getTimestamp().getDayOfWeek().getValue());
        
        if (transaction.getLocation() != null) {
            features.put("latitude", transaction.getLocation().getLatitude());
            features.put("longitude", transaction.getLocation().getLongitude());
        }
        
        // User statistics if available
        DescriptiveStatistics userStats = userAmountStats.get(transaction.getUserId());
        if (userStats != null && userStats.getN() > 0) {
            features.put("user_avg_amount", userStats.getMean());
            features.put("user_tx_count", (double) userStats.getN());
        }
        
        return features;
    }

    @Override
    public String getDetectorName() {
        return "Statistical";
    }

    @Override
    public boolean supportsOnlineLearning() {
        return true;
    }

    // Getters for monitoring
    public int getTotalTransactions() {
        return totalTransactions.get();
    }

    public boolean isModelTrained() {
        return modelTrained;
    }

    public int getGlobalSampleCount() {
        return (int) globalAmountStats.getN();
    }

    public double getGlobalMeanAmount() {
        return globalAmountStats.getMean();
    }

    public int getUserCount() {
        return userAmountStats.size();
    }
}