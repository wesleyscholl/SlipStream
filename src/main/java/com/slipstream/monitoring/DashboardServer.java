package com.slipstream.monitoring;

import com.slipstream.model.AnomalyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Simple HTTP server providing a REST API for monitoring dashboard.
 * Provides endpoints for retrieving real-time metrics and system status.
 */
public class DashboardServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardServer.class);
    
    private final MetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;
    private HttpServer server;
    private final int port;
    
    public DashboardServer(MetricsCollector metricsCollector, int port) {
        this.metricsCollector = metricsCollector;
        this.port = port;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    public DashboardServer(MetricsCollector metricsCollector) {
        this(metricsCollector, 8080);
    }
    
    /**
     * Start the dashboard server
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // API endpoints
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/anomalies", new AnomaliesHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/distribution", new DistributionHandler());
        
        // Static content
        server.createContext("/", new StaticHandler());
        
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        
        logger.info("Dashboard server started on port {}", port);
        logger.info("Access dashboard at http://localhost:{}/", port);
    }
    
    /**
     * Stop the dashboard server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Dashboard server stopped");
        }
    }
    
    /**
     * Handler for /api/metrics endpoint
     */
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                MetricsCollector.SystemMetrics metrics = metricsCollector.getCurrentMetrics();
                sendJsonResponse(exchange, metrics, 200);
            } else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }
    
    /**
     * Handler for /api/anomalies endpoint
     */
    private class AnomaliesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                var recentAnomalies = metricsCollector.getRecentAnomalies();
                sendJsonResponse(exchange, recentAnomalies, 200);
            } else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }
    
    /**
     * Handler for /api/health endpoint
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                boolean healthy = metricsCollector.isSystemHealthy();
                Map<String, Object> health = Map.of(
                    "healthy", healthy,
                    "timestamp", LocalDateTime.now(),
                    "processing_rate", metricsCollector.getProcessingRate(),
                    "uptime_check", "OK"
                );
                sendJsonResponse(exchange, health, healthy ? 200 : 503);
            } else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }
    
    /**
     * Handler for /api/distribution endpoint
     */
    private class DistributionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, Integer> distribution = metricsCollector.getAnomalyDistribution();
                sendJsonResponse(exchange, distribution, 200);
            } else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }
    
    /**
     * Handler for static content (dashboard HTML/CSS/JS)
     */
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if ("/".equals(path)) {
                sendHtmlResponse(exchange, generateDashboardHtml(), 200);
            } else {
                sendResponse(exchange, "Not Found", 404);
            }
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, Object data, int statusCode) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(data);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, json.length());
            exchange.getResponseBody().write(json.getBytes());
        } catch (Exception e) {
            logger.error("Error sending JSON response", e);
            sendResponse(exchange, "Internal Server Error", 500);
        } finally {
            exchange.close();
        }
    }
    
    private void sendHtmlResponse(HttpExchange exchange, String html, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(statusCode, html.length());
        exchange.getResponseBody().write(html.getBytes());
        exchange.close();
    }
    
    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }
    
    /**
     * Generate simple HTML dashboard
     */
    private String generateDashboardHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SlipStream Anomaly Detection Dashboard</title>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0; 
                        padding: 20px; 
                        background: #f5f5f5; 
                    }
                    .dashboard { 
                        max-width: 1200px; 
                        margin: 0 auto; 
                    }
                    .header { 
                        background: white; 
                        padding: 20px; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        margin-bottom: 20px;
                    }
                    .metrics-grid { 
                        display: grid; 
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); 
                        gap: 20px; 
                        margin-bottom: 20px;
                    }
                    .metric-card { 
                        background: white; 
                        padding: 20px; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .metric-value { 
                        font-size: 2em; 
                        font-weight: bold; 
                        color: #2563eb; 
                    }
                    .metric-label { 
                        color: #6b7280; 
                        margin-top: 8px; 
                    }
                    .anomaly-list { 
                        background: white; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .anomaly-item { 
                        padding: 15px 20px; 
                        border-bottom: 1px solid #e5e7eb; 
                    }
                    .anomaly-item:last-child { 
                        border-bottom: none; 
                    }
                    .status-indicator { 
                        display: inline-block; 
                        width: 12px; 
                        height: 12px; 
                        border-radius: 50%; 
                        margin-right: 8px;
                    }
                    .status-healthy { background: #10b981; }
                    .status-warning { background: #f59e0b; }
                    .status-error { background: #ef4444; }
                    h1, h2 { margin: 0 0 20px 0; }
                    h1 { color: #1f2937; }
                    h2 { color: #374151; }
                    .loading { text-align: center; color: #6b7280; }
                </style>
            </head>
            <body>
                <div class="dashboard">
                    <div class="header">
                        <h1>SlipStream Anomaly Detection Dashboard</h1>
                        <span id="health-indicator" class="status-indicator"></span>
                        <span id="health-text">Checking system health...</span>
                    </div>
                    
                    <div class="metrics-grid">
                        <div class="metric-card">
                            <div class="metric-value" id="total-transactions">-</div>
                            <div class="metric-label">Total Transactions</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value" id="total-anomalies">-</div>
                            <div class="metric-label">Anomalies Detected</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value" id="anomaly-rate">-</div>
                            <div class="metric-label">Anomaly Rate</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value" id="processing-time">-</div>
                            <div class="metric-label">Avg Processing Time (ms)</div>
                        </div>
                    </div>
                    
                    <div class="anomaly-list">
                        <h2 style="padding: 20px 20px 0 20px;">Recent Anomalies</h2>
                        <div id="anomaly-feed" class="loading">Loading recent anomalies...</div>
                    </div>
                </div>
                
                <script>
                    let refreshInterval;
                    
                    async function fetchMetrics() {
                        try {
                            const [metricsRes, healthRes, anomaliesRes] = await Promise.all([
                                fetch('/api/metrics'),
                                fetch('/api/health'),
                                fetch('/api/anomalies')
                            ]);
                            
                            const metrics = await metricsRes.json();
                            const health = await healthRes.json();
                            const anomalies = await anomaliesRes.json();
                            
                            updateMetrics(metrics);
                            updateHealth(health);
                            updateAnomalies(anomalies);
                        } catch (error) {
                            console.error('Error fetching data:', error);
                            updateHealth({ healthy: false });
                        }
                    }
                    
                    function updateMetrics(metrics) {
                        document.getElementById('total-transactions').textContent = metrics.totalTransactions.toLocaleString();
                        document.getElementById('total-anomalies').textContent = metrics.totalAnomalies.toLocaleString();
                        document.getElementById('anomaly-rate').textContent = (metrics.anomalyRate * 100).toFixed(2) + '%';
                        document.getElementById('processing-time').textContent = metrics.averageProcessingTime.toFixed(1);
                    }
                    
                    function updateHealth(health) {
                        const indicator = document.getElementById('health-indicator');
                        const text = document.getElementById('health-text');
                        
                        if (health.healthy) {
                            indicator.className = 'status-indicator status-healthy';
                            text.textContent = 'System Healthy';
                        } else {
                            indicator.className = 'status-indicator status-error';
                            text.textContent = 'System Issues Detected';
                        }
                    }
                    
                    function updateAnomalies(anomalies) {
                        const feed = document.getElementById('anomaly-feed');
                        
                        if (anomalies.length === 0) {
                            feed.innerHTML = '<div class="anomaly-item">No recent anomalies detected</div>';
                            return;
                        }
                        
                        feed.innerHTML = anomalies.slice(0, 10).map(anomaly => `
                            <div class="anomaly-item">
                                <strong>Transaction ${anomaly.transactionId}</strong> 
                                <span style="float: right; color: #ef4444;">Score: ${anomaly.score.toFixed(3)}</span>
                                <br>
                                <small style="color: #6b7280;">
                                    ${anomaly.type} • ${new Date(anomaly.timestamp).toLocaleString()}
                                </small>
                            </div>
                        `).join('');
                    }
                    
                    // Start monitoring
                    fetchMetrics();
                    refreshInterval = setInterval(fetchMetrics, 5000); // Refresh every 5 seconds
                    
                    // Cleanup on page unload
                    window.addEventListener('beforeunload', () => {
                        if (refreshInterval) clearInterval(refreshInterval);
                    });
                </script>
            </body>
            </html>
            """;
    }
    
    public int getPort() {
        return port;
    }
}