# AI Customer Assistant Documentation

## Overview

The AI Customer Assistant is an automated chatbot that helps clients navigate the Service Booking & Consulting Platform. It provides instant responses to common questions about bookings, payments, services, and policies.

---

## Implementation Details

### Location
- **Service Class:** `src/backend/core/AIChatbotService.java`
- **API Endpoint:** `POST /api/ai/chat`
- **Frontend Component:** `frontend/app.js` (sendMessage function)

### Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Frontend  │────▶│  API Server  │────▶│ AIChatbotService│
│  (app.js)   │◀────│  (ApiServer) │◀────│  (Knowledge Base)│
└─────────────┘     └──────────────┘     └─────────────────┘
```

---

## Features

### What the Chatbot Can Do

| Category | Example Questions | Response Type |
|----------|------------------|---------------|
| **Greetings** | "Hello", "Hi", "Hey" | Welcome message |
| **Booking Help** | "How do I book?", "Reserve appointment" | Step-by-step booking guide |
| **Cancellation** | "Can I cancel?", "Refund policy" | Cancellation rules |
| **Payments** | "Payment methods?", "Credit card?" | Accepted payment types |
| **Services** | "What services?", "Career counseling" | Service catalog info |
| **Consultants** | "Who are consultants?", "Advisor info" | Consultant verification info |
| **Technical** | "Not working", "Error", "Bug" | Troubleshooting tips |
| **Hours** | "When open?", "Available hours" | Platform availability |

### What the Chatbot CANNOT Do

- ❌ Access personal user data
- ❌ View specific booking details
- ❌ Process payments or refunds
- ❌ Modify account information
- ❌ Connect to external AI APIs (Phase 2 uses rule-based responses)
- ❌ Take automated actions on behalf of users

---

## Knowledge Base

The chatbot uses a predefined knowledge base with categorized responses:

### Response Categories

```java
knowledgeBase = {
    "greeting": ["Hello! How can I assist you today?", ...],
    "how_to_book": ["You can browse services from the main menu...", ...],
    "cancellation": ["Our cancellation policy allows full refund...", ...],
    "payment": ["We accept Credit Cards, Debit Cards, PayPal...", ...],
    "services": ["We offer Career Counseling, IT Consulting...", ...],
    "consultant": ["All our consultants are verified professionals...", ...],
    "technical": ["If you're experiencing technical issues...", ...],
    "hours": ["Our system is available 24/7 for booking...", ...],
    "default": ["I'm not sure I understand. Could you rephrase?", ...]
}
```

### Intent Detection

The chatbot uses keyword matching to detect user intent:

| Intent | Keywords |
|--------|----------|
| greeting | hello, hi, hey, greetings, good morning |
| how_to_book | book, booking, reserve, appointment, schedule |
| cancellation | cancel, cancellation, refund, money back |
| payment | pay, payment, credit card, debit card, paypal |
| services | service, consulting, career, it, finance, price |
| consultant | consultant, expert, advisor, professional |
| technical | issue, problem, error, bug, not working |
| hours | hour, time, available, open, close |

---

## API Specification

### Endpoint

```
POST /api/ai/chat
```

### Request

```json
{
  "message": "How do I book a consultation?"
}
```

### Response

```json
{
  "success": true,
  "response": "To book a consultation, go to 'Browse Services' and click 'Book Now' on your preferred service. You'll need to select a consultant and time slot."
}
```

### Error Response

```json
{
  "success": false,
  "error": "Message required"
}
```

---

## Frontend Integration

### Chat UI Component

The chatbot is embedded in the sidebar of the main application:

```html
<div class="chatbot-container">
    <div class="chat-messages" id="chatMessages">
        <!-- Messages appear here -->
    </div>
    <div class="chat-input">
        <input type="text" id="chatInput" placeholder="Ask me anything...">
        <button onclick="sendMessage()">Send</button>
    </div>
</div>
```

### JavaScript Implementation

```javascript
async function sendMessage() {
    const message = document.getElementById('chatInput').value;
    
    // Add user message to UI
    addChatMessage(message, 'user');
    
    // Call API
    const response = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message })
    });
    
    const data = await response.json();
    addChatMessage(data.response, 'bot');
}
```

---

## Privacy & Security

### Data Protection

| Principle | Implementation |
|-----------|---------------|
| **No Personal Data** | Chatbot never receives user ID, email, or booking details |
| **No Database Access** | AI service operates independently from database layer |
| **No Action Execution** | Chatbot only provides information, cannot modify state |
| **Stateless Design** | No conversation history stored between sessions |

### Safe Response Guidelines

The chatbot is designed to:
- ✅ Provide general platform information
- ✅ Explain processes and policies
- ✅ Guide users to appropriate UI sections
- ❌ Never reveal user-specific information
- ❌ Never execute actions on behalf of users
- ❌ Never store conversation history

---

## Testing

### Test Cases

| Test ID | Input | Expected Output |
|---------|-------|-----------------|
| TC-001 | "Hello" | Greeting response |
| TC-002 | "How do I book?" | Booking instructions |
| TC-003 | "Can I get a refund?" | Cancellation policy |
| TC-004 | "What payment methods?" | List of payment options |
| TC-005 | " gibberish" | Default fallback response |
| TC-006 | "" (empty) | Error or fallback response |

### Manual Testing

```bash
# Test via curl
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"How do I book a consultation?"}'
```

---

## Future Enhancements (Phase 3)

### Real LLM Integration

Potential upgrades for future phases:

| Feature | Current (Phase 2) | Future (Phase 3) |
|---------|------------------|------------------|
| **Response Generation** | Rule-based keywords | LLM (OpenAI/Claude) |
| **Context Awareness** | None | Conversation history |
| **Personalization** | Generic responses | User-specific guidance |
| **Multi-language** | English only | Multi-language support |
| **Learning** | Static knowledge base | Continuous improvement |

### Integration Options

```java
// Future Phase 3 implementation example
public String getLLMResponse(String userInput, UserContext context) {
    // Construct prompt with system context
    String prompt = buildSafePrompt(userInput, context);
    
    // Call external AI API
    String response = openAIService.chat(prompt);
    
    // Sanitize and return
    return sanitizeResponse(response);
}
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No response | API server not running | Start backend with `docker-compose up` |
| Wrong response | Keyword not matched | Add keyword to intent detection |
| Slow response | Network latency | Use demo fallback responses |

### Debug Mode

Enable debug logging in `AIChatbotService.java`:

```java
System.out.println("User input: " + userInput);
System.out.println("Detected intent: " + detectedIntent);
System.out.println("Response: " + response);
```

---

## Code Reference

### Key Methods

| Method | Purpose | Location |
|--------|---------|----------|
| `getResponse(String)` | Main entry point for chat | AIChatbotService.java:57 |
| `initializeKnowledgeBase()` | Setup response categories | AIChatbotService.java:15 |
| `containsAny(String, List)` | Keyword matching | AIChatbotService.java:89 |
| `getRandomResponse(String)` | Select response from category | AIChatbotService.java:78 |

---

## Contact

For questions about the AI chatbot implementation, refer to:
- Source code: `src/backend/core/AIChatbotService.java`
- API documentation: `API_DOCUMENTATION.md`
- Main README: `README.md`

---

**Version:** Phase 2 (Rule-based)  
**Last Updated:** March 30, 2026
