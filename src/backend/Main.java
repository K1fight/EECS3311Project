package backend;

import backend.api.ApiServer;

/**
 * Main Application Entry Point
 * Starts the REST API server
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Consulting Booking System API Server...");
        
        ApiServer apiServer = new ApiServer();
        apiServer.start();
        
        System.out.println("Server is ready to accept requests!");
        System.out.println("API Endpoints available at: http://localhost:8080/api");
        
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
