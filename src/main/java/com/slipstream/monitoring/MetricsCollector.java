package com.slipstream.monitoring;

import com.slipstream.model.AnomalyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time metrics collector for anomaly detection system.
 * Tracks performance metrics, detection rates, and system health.
 */
public class MetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);
    
    // Transaction metrics
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong totalAnomalies = new AtomicLong(0);
    private final AtomicLong totalAlerts = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicInteger activeDetectors = new AtomicInteger(0);
    
    // Time-based metrics (last hour)
    private final Map<String, AtomicInteger> hourlyMetrics = new ConcurrentHashMap<>();
    private final Queue<TimestampedMetric> recentAnomalies = new LinkedList<>();
    private final Map<String, AtomicInteger> anomalyTypeCounters = new ConcurrentHashMap<>();
    
    // System health metrics
    private volatile double systemLoad = 0.0;
    private volatile long memoryUsage = 0L;
    private volatile LocalDateTime lastMetricUpdate = LocalDateTime.now();
    
    public MetricsCollector() {
        initializeMetrics();
        logger.info("MetricsCollector initialized");
    }
    
    private void initializeMetrics() {
        // Initialize hourly metrics for the last 24 hours
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 24; i++) {
            String hour = now.minusHours(i).truncatedTo(ChronoUnit.HOURS).toString();
            hourlyMetrics.put(hour, new AtomicInteger(0));
        }
    }
    
    /**
     * Record a processed transaction
     */
    public void recordTransaction(long processingTimeMs) {
        totalTransactions.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
        
        updateHourlyMetrics("transactions");
        lastMetricUpdate = LocalDateTime.now();
    }
    
    /**
     * Record a detected anomaly
     */
    public void recordAnomaly(AnomalyResult result) {
        totalAnomalies.incrementAndGet();
        
        // Record by type
        String anomalyType = result.getAnomalyType().toString().toLowerCase();
        anomalyTypeCounters.computeIfAbsent(anomalyType, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Add to recent anomalies (keep last 100)
        synchronized (recentAnomalies) {
            recentAnomalies.offer(new TimestampedMetric(
                result.getTransactionId(),
                result.getAnomalyScore(),
                result.getAnomalyType().toString(),
                LocalDateTime.now()
            ));
            
            while (recentAnomalies.size() > 100) {
                recentAnomalies.poll();
            }
        }
        
        updateHourlyMetrics("anomalies");
        logger.debug("Recorded anomaly: {} with score {}", 
                    result.getTransactionId(), result.getAnomalyScore());
    }
    
    /**
     * Record an alert sent to external systems
     */
    public void recordAlert(AnomalyResult result) {
        totalAlerts.incrementAndGet();
        updateHourlyMetrics("alerts");
        
        logger.info("Alert recorded for transaction {}", result.getTransactionId());
    }
    
    /**
     * Update system health metrics
     */
    public void updateSystemHealth() {
        // Get system metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        memoryUsage = totalMemory - freeMemory;
        
        // Simple system load approximation
        systemLoad = (double) (totalMemory - freeMemory) / totalMemory;
        
        lastMetricUpdate = LocalDateTime.now();
    }
    
    private void updateHourlyMetrics(String type) {
        String currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toString();
        hourlyMetrics.computeIfAbsent(currentHour + "_" + type, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Get current system metrics
     */
    public SystemMetrics getCurrentMetrics() {
        return new SystemMetrics(
            totalTransactions.get(),
            totalAnomalies.get(),
            totalAlerts.get(),
            calculateAnomalyRate(),
            calculateAverageProcessingTime(),
            activeDetectors.get(),
            systemLoad,
            memoryUsage,
            lastMetricUpdate
        );
    }
    
    /**
     * Get recent anomalies
     */
    public List<TimestampedMetric> getRecentAnomalies() {
        synchronized (recentAnomalies) {
            return new ArrayList<>(recentAnomalies);
        }
    }
    
    /**
     * Get anomaly distribution by type
     */
    public Map<String, Integer> getAnomalyDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        anomalyTypeCounters.forEach((type, counter) -> 
            distribution.put(type, counter.get())
        );
        return distribution;
    }
    
    /**
     * Get hourly transaction metrics for the last 24 hours
     */
    public Map<String, Integer> getHourlyMetrics(String type) {
        Map<String, Integer> metrics = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 24; i++) {
            String hour = now.minusHours(i).truncatedTo(ChronoUnit.HOURS).toString();
            String key = hour + "_" + type;
            metrics.put(hour, hourlyMetrics.getOrDefault(key, new AtomicInteger(0)).get());
        }
        
        return metrics;
    }
    
    /**
     * Calculate current anomaly detection rate
     */
    private double calculateAnomalyRate() {
        long transactions = totalTransactions.get();
        if (transactions == 0) return 0.0;
        return (double) totalAnomalies.get() / transactions;
    }
    
    /**
     * Calculate average processing time per transaction
     */
    private double calculateAverageProcessingTime() {
        long transactions = totalTransactions.get();
        if (transactions == 0) return 0.0;
        return (double) totalProcessingTime.get() / transactions;
    }
    
    /**
     * Check if system is healthy
     */
    public boolean isSystemHealthy() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return lastMetricUpdate.isAfter(threshold) && systemLoad < 0.9;
    }
    
    /**
     * Get processing rate (transactions per second)
     */
    public double getProcessingRate() {
        // Calculate transactions in the last minute
        String lastMinute = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES).toString();
        int recentTransactions = hourlyMetrics.getOrDefault(lastMinute + "_transactions", new AtomicInteger(0)).get();
        return recentTransactions / 60.0; // per second
    }
    
    public void setActiveDetectors(int count) {
        activeDetectors.set(count);
    }
    
    /**
     * Reset all metrics (useful for testing)
     */
    public void reset() {
        totalTransactions.set(0);
        totalAnomalies.set(0);
        totalAlerts.set(0);
        totalProcessingTime.set(0);
        hourlyMetrics.clear();
        recentAnomalies.clear();
        anomalyTypeCounters.clear();
        initializeMetrics();
        logger.info("All metrics reset");
    }
    
    /**
     * Container for timestamped metrics
     */
    public static class TimestampedMetric {
        private final String transactionId;
        private final double score;
        private final String type;
        private final LocalDateTime timestamp;
        
        public TimestampedMetric(String transactionId, double score, String type, LocalDateTime timestamp) {
            this.transactionId = transactionId;
            this.score = score;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getTransactionId() { return transactionId; }
        public double getScore() { return score; }
        public String getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Container for current system metrics
     */
    public static class SystemMetrics {
        private final long totalTransactions;
        private final long totalAnomalies;
        private final long totalAlerts;
        private final double anomalyRate;
        private final double averageProcessingTime;
        private final int activeDetectors;
        private final double systemLoad;
        private final long memoryUsage;
        private final LocalDateTime lastUpdate;
        
        public SystemMetrics(long totalTransactions, long totalAnomalies, long totalAlerts,
                           double anomalyRate, double averageProcessingTime, int activeDetectors,
                           double systemLoad, long memoryUsage, LocalDateTime lastUpdate) {
            this.totalTransactions = totalTransactions;
            this.totalAnomalies = totalAnomalies;
            this.totalAlerts = totalAlerts;
            this.anomalyRate = anomalyRate;
            this.averageProcessingTime = averageProcessingTime;
            this.activeDetectors = activeDetectors;
            this.systemLoad = systemLoad;
            this.memoryUsage = memoryUsage;
            this.lastUpdate = lastUpdate;
        }
        
        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public long getTotalAnomalies() { return totalAnomalies; }
        public long getTotalAlerts() { return totalAlerts; }
        public double getAnomalyRate() { return anomalyRate; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public int getActiveDetectors() { return activeDetectors; }
        public double getSystemLoad() { return systemLoad; }
        public long getMemoryUsage() { return memoryUsage; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
    }
}