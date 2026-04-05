package backend;

import backend.api.ApiServer;

/**
 * Main Application Entry Point
 * Starts the REST API server
 * Configuration loaded from .env file
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Consulting Booking System API Server...");
        
        // Print loaded configuration (for debugging)
        EnvConfig.printConfig();
        
        ApiServer apiServer = new ApiServer();
        apiServer.start();
        
        String apiUrl = EnvConfig.getApiBaseUrl();
        System.out.println("Server is ready to accept requests!");
        System.out.println("API Endpoints available at: " + apiUrl);
        
        // Keep the server running - single threaded approach
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
        }
    }
}
