package com.slipstream.detector;

import com.slipstream.model.TransactionEvent;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User behavioral profile for personalized anomaly detection.
 * Tracks user spending patterns, preferences, and behavioral characteristics.
 */
public class UserProfile {
    
    private final String userId;
    private final DescriptiveStatistics amountStats;
    private final Map<String, Integer> merchantCategories;
    private final Map<String, Integer> paymentMethods;
    private final Map<Integer, Integer> hourFrequency; // hour -> count
    private final Map<DayOfWeek, Integer> dayFrequency;
    private final List<TransactionEvent.Location> locations;
    private final Queue<TransactionEvent> recentTransactions;
    
    private int transactionCount;
    private LocalDateTime lastTransactionTime;
    private double variabilityScore;

    public UserProfile(String userId) {
        this.userId = userId;
        this.amountStats = new DescriptiveStatistics();
        this.merchantCategories = new ConcurrentHashMap<>();
        this.paymentMethods = new ConcurrentHashMap<>();
        this.hourFrequency = new ConcurrentHashMap<>();
        this.dayFrequency = new ConcurrentHashMap<>();
        this.locations = new ArrayList<>();
        this.recentTransactions = new LinkedList<>();
        this.transactionCount = 0;
    }

    /**
     * Add a transaction to this user's profile
     */
    public synchronized void addTransaction(TransactionEvent transaction) {
        // Update amount statistics
        amountStats.addValue(transaction.getAmount());
        
        // Update merchant category frequency
        merchantCategories.merge(transaction.getMerchantCategory(), 1, Integer::sum);
        
        // Update payment method frequency
        paymentMethods.merge(transaction.getPaymentMethod(), 1, Integer::sum);
        
        // Update temporal patterns
        int hour = transaction.getTimestamp().getHour();
        DayOfWeek dayOfWeek = transaction.getTimestamp().getDayOfWeek();
        hourFrequency.merge(hour, 1, Integer::sum);
        dayFrequency.merge(dayOfWeek, 1, Integer::sum);
        
        // Update location history
        if (transaction.getLocation() != null) {
            locations.add(transaction.getLocation());
            // Keep only last 50 locations
            if (locations.size() > 50) {
                locations.remove(0);
            }
        }
        
        // Update recent transactions for velocity analysis
        recentTransactions.offer(transaction);
        if (recentTransactions.size() > 100) {
            recentTransactions.poll();
        }
        
        transactionCount++;
        lastTransactionTime = transaction.getTimestamp();
        
        // Update variability score based on amount standard deviation
        if (amountStats.getN() > 5) {
            double cv = amountStats.getStandardDeviation() / amountStats.getMean();
            variabilityScore = Math.min(cv / 2.0, 1.0); // Normalize coefficient of variation
        }
    }

    /**
     * Calculate Z-score for transaction amount
     */
    public double getAmountZScore(double amount) {
        if (amountStats.getN() < 3) {
            return 0.0;
        }
        
        double mean = amountStats.getMean();
        double stdDev = amountStats.getStandardDeviation();
        
        if (stdDev == 0) {
            return amount == mean ? 0.0 : 3.0;
        }
        
        return Math.abs(amount - mean) / stdDev;
    }

    /**
     * Calculate anomaly score for merchant category
     */
    public double getMerchantCategoryAnomalyScore(String category) {
        if (transactionCount < 5) {
            return 0.0;
        }
        
        int categoryCount = merchantCategories.getOrDefault(category, 0);
        double frequency = (double) categoryCount / transactionCount;
        
        // Low frequency categories are more anomalous
        return Math.max(0.0, 0.8 - frequency * 4.0);
    }

    /**
     * Calculate anomaly score for payment method
     */
    public double getPaymentMethodAnomalyScore(String paymentMethod) {
        if (transactionCount < 5) {
            return 0.0;
        }
        
        int methodCount = paymentMethods.getOrDefault(paymentMethod, 0);
        double frequency = (double) methodCount / transactionCount;
        
        // Unusual payment methods are anomalous
        return Math.max(0.0, 0.7 - frequency * 3.0);
    }

    /**
     * Calculate anomaly score for transaction hour
     */
    public double getHourAnomalyScore(int hour) {
        if (transactionCount < 10) {
            return 0.0;
        }
        
        int hourCount = hourFrequency.getOrDefault(hour, 0);
        double frequency = (double) hourCount / transactionCount;
        
        // Unusual hours are anomalous
        return Math.max(0.0, 0.6 - frequency * 10.0);
    }

    /**
     * Calculate anomaly score for day of week
     */
    public double getDayOfWeekAnomalyScore(DayOfWeek dayOfWeek) {
        if (transactionCount < 10) {
            return 0.0;
        }
        
        int dayCount = dayFrequency.getOrDefault(dayOfWeek, 0);
        double frequency = (double) dayCount / transactionCount;
        
        // Unusual days are anomalous
        return Math.max(0.0, 0.5 - frequency * 7.0);
    }

    /**
     * Calculate anomaly score for location
     */
    public double getLocationAnomalyScore(TransactionEvent.Location location) {
        if (locations.isEmpty()) {
            return 0.0;
        }
        
        // Find minimum distance to any previous location
        double minDistance = locations.stream()
            .mapToDouble(loc -> calculateDistance(location, loc))
            .min()
            .orElse(Double.MAX_VALUE);
        
        // Distances over 100km are considered anomalous
        return Math.min(1.0, minDistance / 100.0);
    }

    /**
     * Calculate distance between two locations (simplified Haversine)
     */
    private double calculateDistance(TransactionEvent.Location loc1, TransactionEvent.Location loc2) {
        double lat1 = Math.toRadians(loc1.getLatitude());
        double lon1 = Math.toRadians(loc1.getLongitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double lon2 = Math.toRadians(loc2.getLongitude());
        
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        
        double a = Math.sin(dlat/2) * Math.sin(dlat/2) + 
                  Math.cos(lat1) * Math.cos(lat2) * 
                  Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return 6371 * c; // Earth radius in km
    }

    // Getters
    public String getUserId() { return userId; }
    public int getTransactionCount() { return transactionCount; }
    public double getAverageAmount() { return amountStats.getMean(); }
    public double getVariabilityScore() { return variabilityScore; }
    public LocalDateTime getLastTransactionTime() { return lastTransactionTime; }
    
    /**
     * Get user's most frequent merchant category
     */
    public String getMostFrequentCategory() {
        return merchantCategories.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }
    
    /**
     * Get user's most frequent payment method
     */
    public String getMostFrequentPaymentMethod() {
        return paymentMethods.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }
}