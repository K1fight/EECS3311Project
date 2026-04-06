package backend.core;

import backend.EnvConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * AI Customer Assistant - Using Alibaba Cloud DashScope (Qwen)
 * Integrated with Alibaba Cloud DashScope (Qwen Turbo) API
 * Configuration loaded from .env file
 */
public class AIChatbotService {
    private final String API_KEY;
    private final String API_URL;
    private final String MODEL;

    private final HttpClient httpClient;
    private final String systemPrompt;

    public AIChatbotService() {
        // Load configuration from .env file
        this.API_KEY = EnvConfig.get("AI_API_KEY");
        this.API_URL = EnvConfig.get("AI_API_URL");
        this.MODEL = EnvConfig.get("AI_MODEL_NAME");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Build system prompt with platform information
        this.systemPrompt = buildSystemPrompt();

        if (API_KEY.isEmpty()) {
            System.err.println("[AIChatbotService] WARNING: AI_API_KEY not set in .env file");
        }
    }

    /**
     * Build system prompt with platform context
     */
    private String buildSystemPrompt() {
        return """
            You are a smart customer support assistant for a Service Booking and Consulting Platform. 
            You will answer questions about the platform features, services offered, payment methods, 
            the booking process, cancellation policies, and service hours. 
            You must NEVER ask for or reveal personal user information, payment details, or private booking data. 
            Keep answers concise, friendly, and professional.

            ## Platform Features
            - Users can register as Client or Consultant
            - Clients can browse services, book consultations, make payments, and manage payment methods
            - Consultants can accept/reject bookings and manage availability
            - Admins can approve consultants and set policies
                
            ## Services Offered
            1. Career Counseling - $100/hour
            2. IT Consulting - $150/hour
            3. Financial Advisory - $200/hour
                
            ## Payment Methods
            - Credit Card
            - Debit Card
            - PayPal
            - Bank Transfer
                
            ## Booking Process
            - 1: Browse services offered
            - 2: Select a consultant and a time slot
            - 3: Pay once booking is confirmed
                
            ## Cancellation Policy
            - Cancel 48+ hours in advance: Full refund
            - Cancel 24-48 hours in advance: 50% refund
            - Cancel within 24 hours: No refund
                
            ## Service Hours
            - System available 24/7
            - Consultants available weekdays 9 AM - 6 PM
                
            Please respond in a friendly and professional manner. If unsure or out of scope, advise user to contact customer support.
    """;
    }

    /**
     * Send chat request to Alibaba Cloud DashScope
     * @param userMessage The user's message
     * @return AI response
     */
    public String chat(String userMessage) {
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);

            // Build messages list
            List<Map<String, String>> messages = new ArrayList<>();

            // System message
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            // User message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // Convert to JSON
            String jsonBody = toJson(requestBody);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response
            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                System.err.println("AI API Error: " + response.statusCode() + " - " + response.body());
                return "Sorry, I cannot answer right now. Please try again later or contact customer support.";
            }

        } catch (Exception e) {
            System.err.println("AI Chat Error: " + e.getMessage());
            return "Sorry, there is a connection issue. Please try again later.";
        }
    }

    /**
     * Parse API response
     */
    private String parseResponse(String responseBody) {
        try {
            // Simple JSON parsing (avoiding external dependencies)
            responseBody = unescapeJson(responseBody);
            int choicesStart = responseBody.indexOf("\"choices\"");
            if (choicesStart == -1) return "Unable to parse response";

            int messageStart = responseBody.indexOf("\"message\"", choicesStart);
            if (messageStart == -1) return "Unable to parse response";

            int contentStart = responseBody.indexOf("\"content\"", messageStart);
            if (contentStart == -1) return "Unable to parse response";

            // Find start and end quotes of content
            int contentQuoteStart = responseBody.indexOf("\"", contentStart + 9);
            if (contentQuoteStart == -1) return "Unable to parse response";

            int contentQuoteEnd = responseBody.indexOf("\"", contentQuoteStart + 1);
            if (contentQuoteEnd == -1) return "Unable to parse response";

            return responseBody.substring(contentQuoteStart + 1, contentQuoteEnd);
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            return "Unable to parse AI response";
        }
    }

    /**
     * Simple Map to JSON conversion (avoiding Jackson/Gson)
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof List) {
                sb.append(listToJson((List<?>) value));
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            if (item instanceof Map) {
                sb.append(toJson((Map<String, Object>) item));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * Test connection
     */
    public boolean testConnection() {
        try {
            String response = chat("Hello");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get response (alias for chat method - for backward compatibility)
     */
    public String getResponse(String message) {
        return chat(message);
    }
}