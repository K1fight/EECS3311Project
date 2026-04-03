# EECS 3311 Project (Phase 1 & Phase 2)
## Service Booking & Consulting Platform

## GitHub URL
https://github.com/K1fight/EECS3311Project.git

---

## 1. What This Project Is

This is a **complete service booking and consulting platform** that connects clients with consultants. The system manages:

- Consulting services catalog
- Booking lifecycle (request → confirm → pending → pay → complete)
- Payment processing (simulated)
- User authentication and authorization
- Admin oversight
- **AI Customer Assistant Chatbot** (Phase 2)

### Actors
| Actor | Capabilities |
|-------|-------------|
| **Client** | Browse services, request bookings, cancel, pay, view history |
| **Consultant** | Accept/reject bookings, complete sessions, manage availability |
| **Admin** | Approve consultants, define system policies, manage users |

---

## 2. Features

### Phase 1 Features
- **Client**
  - Browse consulting services
  - Request a booking
  - Cancel a booking (with policy enforcement)
  - Process payment (simulated)
  - View booking/payment history

- **Consultant**
  - Accept or reject booking requests
  - Complete bookings
  - Manage availability

- **Admin**
  - Approve consultant registrations
  - Define system policies (cancellation, pricing)

### Phase 2 Features
- **CLI frontend** (All core system workflows)
- **Docker Deployment** (3 containers: backend, frontend, database)
- **AI Customer Assistant Chatbot** (rule-based, privacy-safe)

---

## 3. Design Patterns Used

| Pattern | Location | Purpose |
|---------|----------|---------|
| **State Pattern** | `backend/booking/` | Booking lifecycle management (Requested → Confirmed → Paid → Completed) |
| **Factory Method** | `backend/payment/` | Payment method creation (CreditCard, PayPal, BankTransfer, etc.) |
| **Proxy Pattern** | `backend/user/UserProxy.java` | Access control and role checking |
| **Strategy/Policy** | `backend/policy/` | Cancellation rules, pricing strategies |
| **Observer Pattern** | `backend/notification/` | Booking status notifications |

---

## 4. How to Run

**Single command deployment:**

```bash
docker-compose up --build
```

This starts 3 containers:
- `eecs3311-db` - PostgreSQL database (port 5434)
- `eecs3311-backend` - Backend API (port 8080)
- `eecs3311-frontend` - CLI

**Access the application:**
- Frontend: http://localhost:3000
- API: http://localhost:8080/api
- Health check: http://localhost:8080/api/health

**Stop the application:**
```bash
docker-compose down
```

---

## 5. Project Structure

```
EECS3311Project/
├── src/
│   ├── backend/
│   │   ├── api/              # REST API handlers
│   │   ├── booking/          # Booking domain + State pattern
│   │   ├── core/             # Service layer orchestration
│   │   ├── database/         # DAOs + DB connection
│   │   ├── notification/     # Observer pattern
│   │   ├── payment/          # Payment + Factory pattern
│   │   ├── policy/           # Strategy/Policy patterns
│   │   ├── service/          # Service entities
│   │   └── user/             # Users + Proxy pattern
│   └── frontend/
│       ├── BookingUI.java    # UI
│       └── Dockerfile        # Frontend container
├── diagrams/                 # UML diagrams
├── docker-compose.yml        # Docker orchestration
├── nginx.conf                # Frontend server config
├── database.properties       # DB configuration
├── .env.example              # Environment template
└── README.md                 # This file
```

### Package Responsibilities

| Package | Responsibility |
|---------|---------------|
| `booking` | Booking lifecycle, state transitions |
| `core` | Use case orchestration (ClientService, ConsultantService, etc.) |
| `payment` | Payment creation, processing, transaction history |
| `policy` | Business rules (cancellation, pricing, refunds) |
| `user` | User entities, roles, access control |
| `notification` | Simulated notifications |
| `service` | Consulting service catalog |
| `database` | PostgreSQL DAOs and connection management |
| `api` | REST API endpoints |

---

## 6. API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/register` | Register new user |
| POST | `/api/users/login` | User login |
| GET | `/api/users/profile` | Get user profile |

### Services
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/services` | Get all services |
| POST | `/api/services/create` | Create new service |

### Bookings
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bookings/create` | Create booking |
| GET | `/api/bookings/client` | Get client bookings |
| GET | `/api/bookings/consultant` | Get consultant bookings |
| POST | `/api/bookings/confirm` | Confirm booking |
| POST | `/api/bookings/cancel` | Cancel booking |
| POST | `/api/bookings/complete` | Complete booking |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/pay` | Process payment |
| GET | `/api/payments/history` | Get payment history |

### AI Chatbot
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/chat` | Chat with AI assistant |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |

---

## 7. Demo Workflow

### Quick Demo Path (5 minutes)

1. **Start the application:**
   ```bash
   docker-compose up
   ```

2. **Register as Client:**
   - Click "Register" tab
   - Name: `John Doe`
   - Email: `john@example.com`
   - Password: `password123`
   - Account Type: `Client`

3. **Browse Services:**
   - Navigate to "Browse Services"
   - View available consulting services

4. **Request Booking:**
   - Click "Book Now" on a service
   - Select consultant and time slot
   - Submit booking request

5. **Login as Consultant:**
   - Logout and login as consultant
   - Email: `consultant@example.com`
   - Password: `demo123`
   - Account Type: `Consultant`

6. **Accept Booking:**
   - Go to "Booking Requests"
   - Click "Accept" on pending request

7. **Login as Client and Pay:**
   - Logout and login as client
   - Go to "My Bookings"
   - Click "Pay Now" on confirmed booking

8. **Consultant Completes:**
   - Login as consultant
   - Mark booking as completed

9. **Try AI Chatbot:**
    - Ask questions like "How do I book?" or "What payment methods?"

---

## 8. Booking Lifecycle

```
Requested → Confirmed → PendingPayment → Paid → Completed
     ↓          ↓                           ↓
 Rejected   Cancelled                    Cancelled
```

### State Transitions

| From State | Action | To State | Actor |
|------------|--------|----------|-------|
| Requested | Confirm | Confirmed | Consultant |
| Requested | Reject | Rejected | Consultant |
| Confirmed | Cancel | Cancelled | Client |
| Confirmed | Pay | Paid | Client |
| Paid | Complete | Completed | Consultant |
| Paid | Cancel | Cancelled + Refund | Client |

---

## 9. AI Chatbot Documentation

### Purpose
The AI Customer Assistant helps clients with:
- Platform navigation guidance
- Booking process explanations
- Payment method information
- Cancellation policy questions
- General troubleshooting

### Privacy & Safety
- **NO access** to personal user data
- **NO access** to database or booking details
- **NO automated actions** (chatbot only provides information)
- Uses **rule-based responses** from predefined knowledge base

### Implementation
- Located: `backend/core/AIChatbotService.java`
- API Endpoint: `POST /api/ai/chat`
- Response format: `{"success": true, "response": "..."}`

### Example Interactions

| User Question | Chatbot Response |
|--------------|------------------|
| "How do I book?" | "To book a consultation, go to 'Browse Services' and click 'Book Now'..." |
| "Can I cancel?" | "Yes, you can cancel from 'My Bookings'. Full refund if 48+ hours in advance..." |
| "Payment methods?" | "We accept Credit Cards, Debit Cards, PayPal, and Bank Transfers..." |

---

## 10. Team Contributions

| Member | Responsibilities | GitHub |
|--------|-----------------|--------|
| Bin Tang | Backend architecture, design patterns | K1fight |
| Zehao Liu | Frontend, documentation, corrections | liu0205-mario |
| Haiyun He | UML diagrams, backend additions | 3canary |

---

## 11. Configuration

### Environment Variables (.env)

```bash
# Copy .env.example to .env
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 5434 | Database port |
| `DB_NAME` | mydb | Database name |
| `DB_USER` | myuser | Database user |
| `DB_PASSWORD` | mypassword | Database password |
| `SERVER_PORT` | 8080 | API server port |
| `FRONTEND_PORT` | 3000 | Frontend port |

---

## 12. Known Limitations

### Phase 1
- In-memory storage (no persistence without Docker)
- CLI frontend with basic features

### Phase 2
- Simulated payments (no real payment gateway)
- Demo credentials for quick testing

---

## 13. Troubleshooting

### Docker Issues
```bash
# Rebuild containers
docker-compose down
docker-compose up --build

# Check container logs
docker-compose logs app
docker-compose logs db
```

### Database Connection
```bash
# Test database connection
docker exec -it eecs3311-db psql -U myuser -d mydb
```

### API Not Responding
```bash
# Check if server is running
curl http://localhost:8080/api/health
```

---

## 14. Testing

### Manual Testing Checklist
- [ ] User registration (Client/Consultant)
- [ ] User login/logout
- [ ] Browse services
- [ ] Create booking
- [ ] Consultant accept/reject
- [ ] Client payment
- [ ] Booking completion
- [ ] Cancellation with policy
- [ ] AI chatbot responses
- [ ] Admin consultant approval

### API Testing with curl
```bash
# Health check
curl http://localhost:8080/api/health

# Get services
curl http://localhost:8080/api/services

# Login
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}'
```

---

## 15. Future Enhancements (Phase 3)

- Real payment gateway (Stripe/PayPal)
- Email notifications
- Calendar integration
- Video consultation support
- Mobile app

---

## License

York University EECS 3311 Course Project - 2026

---

**Questions?** Check `API_DOCUMENTATION.md` or run `docker-compose logs` for debugging.
