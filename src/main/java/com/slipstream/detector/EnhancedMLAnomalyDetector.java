package com.slipstream.detector;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Machine Learning-based Anomaly Detector with advanced feature engineering.
 * Implements ensemble methods, behavioral profiling, and adaptive thresholds.
 */
public class EnhancedMLAnomalyDetector implements AnomalyDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMLAnomalyDetector.class);
    
    private static final double DEFAULT_ANOMALY_THRESHOLD = 0.75;
    private static final int MIN_TRAINING_SAMPLES = 50;
    private static final int LOOKBACK_DAYS = 30;
    private static final double ENSEMBLE_WEIGHT_STATISTICAL = 0.3;
    private static final double ENSEMBLE_WEIGHT_BEHAVIORAL = 0.4;
    private static final double ENSEMBLE_WEIGHT_TEMPORAL = 0.3;
    
    // Enhanced feature storage
    private final Map<String, UserProfile> userProfiles;
    private final Map<String, MerchantProfile> merchantProfiles;
    private final DescriptiveStatistics globalStats;
    private final Map<String, Queue<TransactionEvent>> userTransactionHistory;
    private final Map<String, Double> adaptiveThresholds;
    
    private final double anomalyThreshold;
    private volatile boolean modelTrained;
    private int totalTransactions;

    public EnhancedMLAnomalyDetector() {
        this(DEFAULT_ANOMALY_THRESHOLD);
    }

    public EnhancedMLAnomalyDetector(double anomalyThreshold) {
        this.anomalyThreshold = anomalyThreshold;
        this.userProfiles = new ConcurrentHashMap<>();
        this.merchantProfiles = new ConcurrentHashMap<>();
        this.globalStats = new DescriptiveStatistics();
        this.userTransactionHistory = new ConcurrentHashMap<>();
        this.adaptiveThresholds = new ConcurrentHashMap<>();
        this.modelTrained = false;
        this.totalTransactions = 0;
        
        logger.info("Initialized Enhanced ML Anomaly Detector with threshold {}", anomalyThreshold);
    }

    @Override
    public String getDetectorName() {
        return "Enhanced ML";
    }

    @Override
    public AnomalyResult detect(TransactionEvent transaction) {
        if (!modelTrained) {
            logger.debug("Model not yet trained, skipping detection for transaction {}", 
                        transaction.getTransactionId());
            return createNormalResult(transaction);
        }

        try {
            // Calculate ensemble anomaly score
            double ensembleScore = calculateEnsembleScore(transaction);
            
            // Get adaptive threshold for user
            double userThreshold = getAdaptiveThreshold(transaction.getUserId());
            
            boolean isAnomaly = ensembleScore > userThreshold;
            AnomalyResult.AnomalyType anomalyType = determineAnomalyType(transaction, ensembleScore);
            
            Map<String, Double> features = extractFeatures(transaction);
            String reason = generateExplanation(transaction, ensembleScore, features);
            
            logger.debug("Transaction {} - Score: {:.3f}, Threshold: {:.3f}, Anomaly: {}", 
                        transaction.getTransactionId(), ensembleScore, userThreshold, isAnomaly);

            return new AnomalyResult(
                transaction.getTransactionId(),
                isAnomaly,
                ensembleScore,
                calculateConfidence(ensembleScore, userThreshold),
                anomalyType,
                LocalDateTime.now(),
                transaction,
                features,
                reason
            );

        } catch (Exception e) {
            logger.error("Error detecting anomaly for transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
            return createNormalResult(transaction);
        }
    }

    /**
     * Calculate ensemble anomaly score using multiple detection methods
     */
    private double calculateEnsembleScore(TransactionEvent transaction) {
        double statisticalScore = calculateStatisticalScore(transaction);
        double behavioralScore = calculateBehavioralScore(transaction);
        double temporalScore = calculateTemporalScore(transaction);
        
        return ENSEMBLE_WEIGHT_STATISTICAL * statisticalScore +
               ENSEMBLE_WEIGHT_BEHAVIORAL * behavioralScore +
               ENSEMBLE_WEIGHT_TEMPORAL * temporalScore;
    }

    /**
     * Calculate statistical anomaly score based on Z-scores and distributions
     */
    private double calculateStatisticalScore(TransactionEvent transaction) {
        double score = 0.0;
        int components = 0;
        
        UserProfile profile = userProfiles.get(transaction.getUserId());
        if (profile != null) {
            // Amount anomaly
            double amountZScore = profile.getAmountZScore(transaction.getAmount());
            score += Math.min(Math.abs(amountZScore) / 3.0, 1.0);
            components++;
            
            // Frequency anomaly
            double frequencyScore = calculateFrequencyAnomaly(transaction, profile);
            score += frequencyScore;
            components++;
        }
        
        return components > 0 ? score / components : 0.0;
    }

    /**
     * Calculate behavioral anomaly score based on user patterns
     */
    private double calculateBehavioralScore(TransactionEvent transaction) {
        UserProfile profile = userProfiles.get(transaction.getUserId());
        if (profile == null) {
            return 0.0;
        }
        
        double score = 0.0;
        int components = 0;
        
        // Merchant category deviation
        double categoryScore = profile.getMerchantCategoryAnomalyScore(transaction.getMerchantCategory());
        score += categoryScore;
        components++;
        
        // Payment method deviation
        double paymentScore = profile.getPaymentMethodAnomalyScore(transaction.getPaymentMethod());
        score += paymentScore;
        components++;
        
        // Location deviation
        if (transaction.getLocation() != null) {
            double locationScore = profile.getLocationAnomalyScore(transaction.getLocation());
            score += locationScore;
            components++;
        }
        
        return components > 0 ? score / components : 0.0;
    }

    /**
     * Calculate temporal anomaly score based on time patterns
     */
    private double calculateTemporalScore(TransactionEvent transaction) {
        UserProfile profile = userProfiles.get(transaction.getUserId());
        if (profile == null) {
            return 0.0;
        }
        
        LocalDateTime timestamp = transaction.getTimestamp();
        int hour = timestamp.getHour();
        DayOfWeek dayOfWeek = timestamp.getDayOfWeek();
        
        double hourScore = profile.getHourAnomalyScore(hour);
        double dayScore = profile.getDayOfWeekAnomalyScore(dayOfWeek);
        double velocityScore = calculateVelocityScore(transaction);
        
        return (hourScore + dayScore + velocityScore) / 3.0;
    }

    /**
     * Calculate velocity-based anomaly score
     */
    private double calculateVelocityScore(TransactionEvent transaction) {
        Queue<TransactionEvent> history = userTransactionHistory.get(transaction.getUserId());
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        
        LocalDateTime now = transaction.getTimestamp();
        long recentTransactions = history.stream()
            .mapToLong(t -> ChronoUnit.MINUTES.between(t.getTimestamp(), now))
            .filter(minutes -> minutes <= 5)
            .count();
        
        // More than 3 transactions in 5 minutes is suspicious
        return Math.min(recentTransactions / 3.0, 1.0);
    }

    @Override
    public void updateModel(TransactionEvent transaction) {
        try {
            synchronized (this) {
                // Update global statistics
                globalStats.addValue(transaction.getAmount());
                totalTransactions++;
                
                // Update user profile
                updateUserProfile(transaction);
                
                // Update merchant profile
                updateMerchantProfile(transaction);
                
                // Update transaction history
                updateTransactionHistory(transaction);
                
                // Update adaptive thresholds
                updateAdaptiveThreshold(transaction);
                
                // Check if model is ready for training
                if (!modelTrained && totalTransactions >= MIN_TRAINING_SAMPLES) {
                    modelTrained = true;
                    logger.info("Enhanced ML model is now trained with {} samples", totalTransactions);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating model: {}", e.getMessage(), e);
        }
    }

    /**
     * Update or create user behavioral profile
     */
    private void updateUserProfile(TransactionEvent transaction) {
        UserProfile profile = userProfiles.computeIfAbsent(
            transaction.getUserId(), 
            userId -> new UserProfile(userId)
        );
        profile.addTransaction(transaction);
    }

    /**
     * Update or create merchant profile
     */
    private void updateMerchantProfile(TransactionEvent transaction) {
        MerchantProfile profile = merchantProfiles.computeIfAbsent(
            transaction.getMerchantId(),
            merchantId -> new MerchantProfile(merchantId)
        );
        profile.addTransaction(transaction);
    }

    /**
     * Update transaction history for velocity analysis
     */
    private void updateTransactionHistory(TransactionEvent transaction) {
        Queue<TransactionEvent> history = userTransactionHistory.computeIfAbsent(
            transaction.getUserId(),
            userId -> new LinkedList<>()
        );
        
        history.offer(transaction);
        
        // Keep only recent transactions (last 24 hours)
        LocalDateTime cutoff = transaction.getTimestamp().minusHours(24);
        history.removeIf(t -> t.getTimestamp().isBefore(cutoff));
    }

    /**
     * Update adaptive threshold for user
     */
    private void updateAdaptiveThreshold(TransactionEvent transaction) {
        String userId = transaction.getUserId();
        UserProfile profile = userProfiles.get(userId);
        
        if (profile != null && profile.getTransactionCount() >= 10) {
            // Adjust threshold based on user's historical variability
            double userVariability = profile.getVariabilityScore();
            double baseThreshold = anomalyThreshold;
            
            // Users with higher variability get higher thresholds
            double adjustedThreshold = baseThreshold + (userVariability * 0.2);
            adaptiveThresholds.put(userId, Math.min(adjustedThreshold, 0.95));
        }
    }

    private double getAdaptiveThreshold(String userId) {
        return adaptiveThresholds.getOrDefault(userId, anomalyThreshold);
    }

    private double calculateFrequencyAnomaly(TransactionEvent transaction, UserProfile profile) {
        // Implementation for frequency-based anomaly detection
        return 0.0; // Placeholder
    }

    private AnomalyResult.AnomalyType determineAnomalyType(TransactionEvent transaction, double score) {
        // Determine the primary type of anomaly based on which component scored highest
        return AnomalyResult.AnomalyType.STATISTICAL_OUTLIER; // Placeholder
    }

    private Map<String, Double> extractFeatures(TransactionEvent transaction) {
        Map<String, Double> features = new HashMap<>();
        features.put("amount", transaction.getAmount());
        features.put("hour_of_day", (double) transaction.getTimestamp().getHour());
        features.put("day_of_week", (double) transaction.getTimestamp().getDayOfWeek().getValue());
        
        UserProfile profile = userProfiles.get(transaction.getUserId());
        if (profile != null) {
            features.put("user_avg_amount", profile.getAverageAmount());
            features.put("user_transaction_count", (double) profile.getTransactionCount());
        }
        
        return features;
    }

    private String generateExplanation(TransactionEvent transaction, double score, Map<String, Double> features) {
        if (score > anomalyThreshold) {
            return String.format("Anomalous transaction detected with score %.3f", score);
        } else {
            return "Normal transaction pattern";
        }
    }

    private double calculateConfidence(double score, double threshold) {
        return Math.min(0.9, 0.5 + Math.abs(score - threshold));
    }

    private AnomalyResult createNormalResult(TransactionEvent transaction) {
        return new AnomalyResult(
            transaction.getTransactionId(),
            false,
            0.1,
            0.8,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.now(),
            transaction,
            new HashMap<>(),
            "Model not trained - default normal"
        );
    }

    @Override
    public boolean supportsOnlineLearning() {
        return true;
    }

    public Map<String, Object> getModelMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_transactions", totalTransactions);
        metrics.put("model_trained", modelTrained);
        metrics.put("unique_users", userProfiles.size());
        metrics.put("unique_merchants", merchantProfiles.size());
        metrics.put("avg_adaptive_threshold", 
                    adaptiveThresholds.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(anomalyThreshold));
        return metrics;
    }
}