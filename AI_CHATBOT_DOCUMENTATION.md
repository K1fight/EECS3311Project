# AI Customer Assistant Documentation

## Overview

The AI Customer Assistant is an intelligent chatbot integrated with **Alibaba Cloud DashScope (Qwen Turbo)** API. It provides natural language responses to help clients navigate the Service Booking & Consulting Platform.

---

## Implementation Details

### Location
- **Service Class:** `src/backend/core/AIChatbotService.java`
- **API Endpoint:** `POST /api/ai/chat`
- **Frontend Component:** `frontend/app.js` (sendMessage function)

### Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────────┐
│   Frontend  │────▶│  API Server  │────▶│  AIChatbotService         │
│  (app.js)   │◀────│  (ApiServer) │◀────│  (DashScope API Client)   │
└─────────────┘     └──────────────┘     └──────────────────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │  Qwen Turbo  │
                                            │  (Aliyun)   │
                                            └──────────────┘
```

---

## Features

### What the Chatbot Can Do

| Category | Example Questions | Response Type |
|----------|------------------|---------------|
| **Greetings** | "Hello", "Hi there" | Friendly greeting |
| **Booking Help** | "How do I book?", "Reserve appointment" | Step-by-step booking guide |
| **Cancellation** | "Can I cancel?", "Refund policy" | Cancellation rules |
| **Payments** | "Payment methods?", "Credit card?" | Accepted payment types |
| **Services** | "What services?", "Career counseling" | Service catalog info |
| **Consultants** | "Who are consultants?" | Consultant information |
| **Technical** | "Not working", "Error" | Troubleshooting tips |
| **General** | Any natural language question | Contextual AI response |

### What the Chatbot CANNOT Do

- ❌ Access personal user data
- ❌ View specific booking details
- ❌ Process payments or refunds
- ❌ Modify account information
- ❌ Take automated actions on behalf of users

---

## Configuration

### Environment Variables (.env)

The chatbot is configured via the `.env` file:

```bash
# Alibaba Cloud DashScope Configuration
AI_API_KEY=your_api_key_here
AI_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
AI_MODEL_NAME=qwen-turbo
```

### Configuration Loading

The service loads configuration from `.env` using `EnvConfig`:

```java
this.API_KEY = EnvConfig.get("AI_API_KEY");
this.API_URL = EnvConfig.get("AI_API_URL");
this.MODEL = EnvConfig.get("AI_MODEL_NAME");
```

---

## System Prompt

The chatbot uses a carefully crafted system prompt that provides platform context:

```
You are a smart customer service assistant for a consulting booking platform.

## Platform Features
- Users can register as Client or Consultant
- Clients can browse services, book consultations, make payments, and manage payment methods
- Consultants can accept/reject bookings and manage availability
- Admins can approve consultants and set policies

## Service Types
1. Career Counseling - $100/hour
2. IT Consulting - $150/hour
3. Financial Advisory - $200/hour

## Payment Methods
- Credit Card, Debit Card, PayPal, Bank Transfer

## Cancellation Policy
- Cancel 48+ hours in advance: Full refund
- Cancel 24-48 hours in advance: 50% refund
- Cancel within 24 hours: No refund

## Service Hours
- System available 24/7
- Consultants available weekdays 9 AM - 6 PM
```

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
  "response": "To book a consultation, first register as a client and log in..."
}
```

### Error Responses

| Error | Cause | Response |
|-------|-------|----------|
| API Key missing | `AI_API_KEY` not set | "Sorry, I cannot answer right now..." |
| API Error | DashScope returns error | "Sorry, I cannot answer right now. Please try again later..." |
| Network Error | Connection failure | "Sorry, there is a connection issue. Please try again later." |

---

## Code Reference

### Class: AIChatbotService

```java
public class AIChatbotService {
    private final String API_KEY;
    private final String API_URL;
    private final String MODEL;
    private final HttpClient httpClient;
    private final String systemPrompt;
    
    public AIChatbotService() { ... }
    public String chat(String userMessage) { ... }
    public String getResponse(String message) { ... }
    public boolean testConnection() { ... }
}
```

### Key Methods

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `chat(String userMessage)` | Send message to AI and get response | `String` |
| `getResponse(String message)` | Alias for chat (backward compatibility) | `String` |
| `testConnection()` | Test if AI service is accessible | `boolean` |

### Internal Methods

| Method | Purpose |
|--------|---------|
| `buildSystemPrompt()` | Construct platform context prompt |
| `parseResponse(String)` | Extract content from DashScope response |
| `toJson(Map)` | Convert Map to JSON string |
| `escapeJson(String)` | Escape special characters for JSON |

---

## Privacy & Security

### Data Protection

| Principle | Implementation |
|-----------|---------------|
| **No Personal Data** | Chatbot never receives user ID, email, or booking details |
| **No Database Access** | AI service operates independently from database layer |
| **No Action Execution** | Chatbot only provides information, cannot modify state |
| **Safe Prompt** | System prompt designed to prevent data leakage |
| **API Key Security** | Key stored in .env, not hardcoded |

### Safety Guidelines

The chatbot is designed to:
- ✅ Provide general platform information
- ✅ Explain processes and policies
- ✅ Guide users to appropriate UI sections
- ✅ Respond in friendly and professional manner
- ❌ Never reveal user-specific information
- ❌ Never execute actions on behalf of users
- ❌ Never store conversation history

---

## Setup Instructions

### 1. Get Alibaba Cloud DashScope API Key

1. Sign up at [Alibaba Cloud](https://www.aliyun.com/)
2. Navigate to DashScope console
3. Create an API key for Qwen Turbo model

### 2. Configure Environment

Edit `.env` file in the project root:

```bash
AI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
AI_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
AI_MODEL_NAME=qwen-turbo
```

### 3. Start the Application

```bash
docker-compose up
```

---

## Testing

### Test Cases

| Test ID | Input | Expected Behavior |
|---------|-------|------------------|
| TC-001 | "Hello" | Friendly greeting response |
| TC-002 | "How do I book?" | Booking instructions |
| TC-003 | "Can I get a refund?" | Cancellation policy explanation |
| TC-004 | "What payment methods?" | List of payment options |
| TC-005 | " gibberish" | Coherent AI response |
| TC-006 | "" (empty) | Error handling |

### Manual Testing

```bash
# Test via curl
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"How do I book a consultation?"}'
```

### Debug Mode

Enable debug logging by checking the console output:

```bash
# AI API responses are logged
docker-compose logs app | grep "AI"
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Sorry, I cannot answer..." | `AI_API_KEY` not set | Set `AI_API_KEY` in `.env` file |
| Slow response | Network latency | Check internet connection |
| Generic response | API limits | Check DashScope quota |
| Connection error | Firewall/proxy | Check network settings |

### Debug Steps

1. Check if `.env` file exists and contains `AI_API_KEY`
2. Verify API key is valid in DashScope console
3. Check Docker logs: `docker-compose logs app`
4. Test API key directly with curl

---

## Future Enhancements

### Potential Upgrades

| Feature | Current | Future |
|---------|---------|--------|
| **Model** | Qwen Turbo | Qwen Max / GPT-4 |
| **Context** | Single message | Conversation history |
| **Personalization** | Generic | User-specific |
| **Languages** | English | Multi-language |

### Integration Example (Future)

```java
public String chatWithHistory(String userMessage, List<Message> history) {
    List<Map<String, String>> messages = new ArrayList<>();
    
    // Add conversation history
    for (Message msg : history) {
        Map<String, String> m = new HashMap<>();
        m.put("role", msg.isUser() ? "user" : "assistant");
        m.put("content", msg.getContent());
        messages.add(m);
    }
    
    // Add current message
    Map<String, String> current = new HashMap<>();
    current.put("role", "user");
    current.put("content", userMessage);
    messages.add(current);
    
    // Send to API...
}
```

---

## Contact

For questions about the AI chatbot implementation, refer to:
- Source code: `src/backend/core/AIChatbotService.java`
- Main README: `README.md`
- Docker setup: `DOCKER_README.md`

---

**Version:** Phase 2 (Alibaba Cloud DashScope Qwen Turbo)  
**Last Updated:** April 6, 2026
