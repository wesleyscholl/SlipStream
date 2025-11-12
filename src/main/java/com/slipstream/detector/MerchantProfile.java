package com.slipstream.detector;

import com.slipstream.model.TransactionEvent;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Merchant profile for tracking merchant-specific patterns and risk indicators.
 * Used to identify suspicious merchant behavior and transaction patterns.
 */
public class MerchantProfile {
    
    private final String merchantId;
    private final DescriptiveStatistics amountStats;
    private final Map<String, Integer> paymentMethodCounts;
    private final DescriptiveStatistics transactionIntervalStats;
    
    private int transactionCount;
    private int uniqueUsers;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private double riskScore;

    public MerchantProfile(String merchantId) {
        this.merchantId = merchantId;
        this.amountStats = new DescriptiveStatistics();
        this.paymentMethodCounts = new ConcurrentHashMap<>();
        this.transactionIntervalStats = new DescriptiveStatistics();
        this.transactionCount = 0;
        this.uniqueUsers = 0;
        this.riskScore = 0.0;
    }

    /**
     * Add a transaction to this merchant's profile
     */
    public synchronized void addTransaction(TransactionEvent transaction) {
        // Update amount statistics
        amountStats.addValue(transaction.getAmount());
        
        // Update payment method tracking
        paymentMethodCounts.merge(transaction.getPaymentMethod(), 1, Integer::sum);
        
        // Update timing
        if (firstSeen == null) {
            firstSeen = transaction.getTimestamp();
        } else {
            // Calculate interval between transactions
            long intervalMinutes = java.time.Duration.between(lastSeen, transaction.getTimestamp()).toMinutes();
            if (intervalMinutes > 0) {
                transactionIntervalStats.addValue(intervalMinutes);
            }
        }
        lastSeen = transaction.getTimestamp();
        
        transactionCount++;
        
        // Update risk score based on various factors
        updateRiskScore();
    }

    /**
     * Update merchant risk score based on transaction patterns
     */
    private void updateRiskScore() {
        double score = 0.0;
        
        // High transaction frequency increases risk
        if (transactionIntervalStats.getN() > 10) {
            double avgInterval = transactionIntervalStats.getMean();
            if (avgInterval < 1.0) { // Less than 1 minute between transactions
                score += 0.3;
            }
        }
        
        // High amount variability increases risk
        if (amountStats.getN() > 10) {
            double cv = amountStats.getStandardDeviation() / amountStats.getMean();
            if (cv > 2.0) { // High coefficient of variation
                score += 0.2;
            }
        }
        
        // Unusual payment method distribution
        if (!paymentMethodCounts.isEmpty()) {
            double maxFreq = paymentMethodCounts.values().stream()
                .mapToDouble(Integer::doubleValue)
                .max()
                .orElse(0.0);
            double uniformity = maxFreq / transactionCount;
            if (uniformity < 0.3) { // Very distributed payment methods
                score += 0.2;
            }
        }
        
        // New merchants are riskier
        if (transactionCount < 50) {
            score += 0.1;
        }
        
        riskScore = Math.min(1.0, score);
    }

    /**
     * Get anomaly score for a transaction amount at this merchant
     */
    public double getAmountAnomalyScore(double amount) {
        if (amountStats.getN() < 5) {
            return 0.0;
        }
        
        double mean = amountStats.getMean();
        double stdDev = amountStats.getStandardDeviation();
        
        if (stdDev == 0) {
            return amount == mean ? 0.0 : 0.8;
        }
        
        double zScore = Math.abs(amount - mean) / stdDev;
        return Math.min(1.0, zScore / 3.0);
    }

    /**
     * Check if merchant shows signs of suspicious activity
     */
    public boolean isSuspicious() {
        return riskScore > 0.7;
    }

    // Getters
    public String getMerchantId() { return merchantId; }
    public int getTransactionCount() { return transactionCount; }
    public double getAverageAmount() { return amountStats.getMean(); }
    public double getRiskScore() { return riskScore; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    
    /**
     * Get merchant's most common payment method
     */
    public String getMostCommonPaymentMethod() {
        return paymentMethodCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }
    
    /**
     * Get average transaction interval in minutes
     */
    public double getAverageTransactionInterval() {
        return transactionIntervalStats.getMean();
    }
}