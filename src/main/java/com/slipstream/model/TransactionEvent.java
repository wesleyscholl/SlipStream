package com.slipstream.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a transaction event in the streaming data pipeline.
 * This is the primary data model for incoming transaction data.
 */
public class TransactionEvent {
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("merchant_id")
    private String merchantId;
    
    @JsonProperty("amount")
    private double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("location")
    private Location location;
    
    @JsonProperty("payment_method")
    private String paymentMethod;
    
    @JsonProperty("merchant_category")
    private String merchantCategory;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public TransactionEvent() {}

    public TransactionEvent(String transactionId, String userId, String merchantId, 
                          double amount, String currency, LocalDateTime timestamp,
                          Location location, String paymentMethod, String merchantCategory,
                          Map<String, Object> metadata) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.location = location;
        this.paymentMethod = paymentMethod;
        this.merchantCategory = merchantCategory;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionEvent that = (TransactionEvent) o;
        return Double.compare(that.amount, amount) == 0 &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(merchantId, that.merchantId) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(location, that.location) &&
                Objects.equals(paymentMethod, that.paymentMethod) &&
                Objects.equals(merchantCategory, that.merchantCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, userId, merchantId, amount, currency, 
                          timestamp, location, paymentMethod, merchantCategory);
    }

    @Override
    public String toString() {
        return "TransactionEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                ", location=" + location +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", merchantCategory='" + merchantCategory + '\'' +
                '}';
    }

    /**
     * Location data for the transaction
     */
    public static class Location {
        @JsonProperty("latitude")
        private double latitude;
        
        @JsonProperty("longitude")
        private double longitude;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("city")
        private String city;

        public Location() {}

        public Location(double latitude, double longitude, String country, String city) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.country = country;
            this.city = city;
        }

        // Getters and Setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return Double.compare(location.latitude, latitude) == 0 &&
                    Double.compare(location.longitude, longitude) == 0 &&
                    Objects.equals(country, location.country) &&
                    Objects.equals(city, location.city);
        }

        @Override
        public int hashCode() {
            return Objects.hash(latitude, longitude, country, city);
        }

        @Override
        public String toString() {
            return "Location{" +
                    "latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    '}';
        }
    }
}