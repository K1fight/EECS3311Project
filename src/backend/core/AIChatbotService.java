package backend.core;

import java.util.*;

/**
 * AI Customer Assistant Chatbot
 * Provides automated responses to common client queries
 */
public class AIChatbotService {
    private Map<String, List<String>> knowledgeBase;
    private Random random = new Random();
    
    public AIChatbotService() {
        initializeKnowledgeBase();
    }
    
    private void initializeKnowledgeBase() {
        knowledgeBase = new HashMap<>();
        
        // Greetings
        List<String> greetings = Arrays.asList(
            "Hello! How can I assist you today?",
            "Hi there! Welcome to our consulting service.",
            "Greetings! What can I help you with?",
            "Hello! Feel free to ask me anything about our services."
        );
        knowledgeBase.put("greeting", greetings);
        
        // Booking information
        List<String> bookingInfo = Arrays.asList(
            "You can browse services from the main menu, select a consultant, and choose your preferred time slot.",
            "To book a consultation, go to 'Request Booking' from your client menu. You'll need to select a service, consultant, and time.",
            "Booking is easy! Just select the service you need, choose an available consultant, and pick a time that works for you."
        );
        knowledgeBase.put("how_to_book", bookingInfo);
        
        // Cancellation policy
        List<String> cancellationPolicy = Arrays.asList(
            "Our cancellation policy allows full refund if cancelled 48 hours before the appointment, and 50% refund if cancelled 24 hours before.",
            "You can cancel your booking from the 'Cancel Booking' menu. Refunds are processed according to our cancellation policy.",
            "Cancellations made 48+ hours in advance receive full refund. Cancellations 24-48 hours before receive 50% refund."
        );
        knowledgeBase.put("cancellation", cancellationPolicy);
        
        // Payment methods
        List<String> paymentMethods = Arrays.asList(
            "We accept Credit Cards, Debit Cards, PayPal, and Bank Transfers.",
            "You can manage your payment methods in the 'Manage Payment Methods' section. We support all major credit cards and digital payment platforms.",
            "For payments, we accept Visa, MasterCard, American Express, PayPal, and direct bank transfers."
        );
        knowledgeBase.put("payment", paymentMethods);
        
        // Service categories
        List<String> services = Arrays.asList(
            "We offer Career Counseling, IT Consulting, and Financial Advisory services.",
            "Our consultants specialize in Career development, Technology, and Finance sectors.",
            "You can choose from Career Counseling ($100/hr), IT Consulting ($150/hr), or Financial Advisory ($200/hr)."
        );
        knowledgeBase.put("services", services);
        
        // Consultant approval
        List<String> consultantApproval = Arrays.asList(
            "All our consultants are verified professionals with extensive industry experience.",
            "Consultants must be approved by our admin team before they can accept bookings.",
            "Our consultants undergo a rigorous verification process to ensure quality service."
        );
        knowledgeBase.put("consultant", consultantApproval);
        
        // Technical support
        List<String> technicalSupport = Arrays.asList(
            "If you're experiencing technical issues, please try refreshing the page or logging out and back in.",
            "For technical support, you can contact our support team at support@consulting.com.",
            "Having issues? Try clearing your browser cache or using a different browser."
        );
        knowledgeBase.put("technical", technicalSupport);
        
        // Hours of operation
        List<String> hours = Arrays.asList(
            "Our system is available 24/7 for booking. Consultants typically work during business hours (9 AM - 6 PM).",
            "You can book consultations anytime through this system. Available time slots depend on individual consultant availability.",
            "The booking platform is always open! Consultant availability varies, but most are available weekdays 9 AM - 6 PM."
        );
        knowledgeBase.put("hours", hours);
        
        // Default responses
        List<String> defaultResponses = Arrays.asList(
            "I'm not sure I understand. Could you rephrase that? Or you can ask about bookings, payments, services, or cancellations.",
            "That's an interesting question. For specific inquiries, please contact our support team. Is there anything else I can help with?",
            "I'm still learning! You can ask me about how to book, payment options, cancellation policy, or our services."
        );
        knowledgeBase.put("default", defaultResponses);
    }
    
    /**
     * Get a response from the chatbot based on user input
     * @param userInput The user's message
     * @return Chatbot's response
     */
    public String getResponse(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return getRandomResponse("default");
        }
        
        String input = userInput.toLowerCase();
        
        // Check for different intents
        if (containsAny(input, Arrays.asList("hello", "hi", "hey", "greetings", "good morning", "good afternoon"))) {
            return getRandomResponse("greeting");
        }
        
        if (containsAny(input, Arrays.asList("book", "booking", "reserve", "appointment", "schedule"))) {
            return getRandomResponse("how_to_book");
        }
        
        if (containsAny(input, Arrays.asList("cancel", "cancellation", "refund", "money back"))) {
            return getRandomResponse("cancellation");
        }
        
        if (containsAny(input, Arrays.asList("pay", "payment", "credit card", "debit card", "paypal", "bank transfer"))) {
            return getRandomResponse("payment");
        }
        
        if (containsAny(input, Arrays.asList("service", "consulting", "career", "it", "finance", "price", "cost"))) {
            return getRandomResponse("services");
        }
        
        if (containsAny(input, Arrays.asList("consultant", "expert", "advisor", "professional"))) {
            return getRandomResponse("consultant");
        }
        
        if (containsAny(input, Arrays.asList("issue", "problem", "error", "bug", "not working", "technical"))) {
            return getRandomResponse("technical");
        }
        
        if (containsAny(input, Arrays.asList("hour", "time", "available", "open", "close"))) {
            return getRandomResponse("hours");
        }
        
        // Default response if no match found
        return getRandomResponse("default");
    }
    
    /**
     * Get a random response from a specific category
     */
    private String getRandomResponse(String category) {
        List<String> responses = knowledgeBase.get(category);
        if (responses != null && !responses.isEmpty()) {
            return responses.get(random.nextInt(responses.size()));
        }
        return "I'm sorry, I don't have information about that.";
    }
    
    /**
     * Check if input contains any of the keywords
     */
    private boolean containsAny(String input, List<String> keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Display help information about what the chatbot can do
     */
    public void displayHelp() {
        System.out.println("\n=== AI Customer Assistant Help ===");
        System.out.println("I can help you with:");
        System.out.println("- Booking information (ask about 'booking', 'reserve', 'appointment')");
        System.out.println("- Cancellation policy (ask about 'cancel', 'refund')");
        System.out.println("- Payment methods (ask about 'payment', 'credit card', 'paypal')");
        System.out.println("- Our services (ask about 'services', 'career', 'IT', 'finance')");
        System.out.println("- Consultants (ask about 'consultant', 'advisor')");
        System.out.println("- Technical support (ask about 'issue', 'problem', 'error')");
        System.out.println("- Operating hours (ask about 'hours', 'available', 'open')");
        System.out.println("\nType 'quit' or 'exit' to leave the chat.");
        System.out.println("=================================\n");
    }
}
