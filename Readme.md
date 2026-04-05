# EECS 3311 Project (Phase 1 & Phase 2)
## Service Booking & Consulting Platform

## GitHub URL
https://github.com/K1fight/EECS3311Project/tree/phase2

---

## 1. What This Project Is

This is a **complete service booking and consulting platform** that connects clients with consultants. The system manages:

- Consulting services catalog
- Booking lifecycle (request → confirm → pay → complete)
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

### Phase 1 Features ✅
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

### Phase 2 Features ✅
- **Docker Deployment** (4 containers: backend, frontend, database, builder)
- **RESTful API Server** (Embedded HTTP server with JSON API)
- **AI Customer Assistant Chatbot** ( privacy-safe, connected to LLM)

---

## 3. Design Patterns Used

| Pattern | Location | Purpose |
|---------|----------|---------|
| **State Pattern** | `backend/booking/` | Booking lifecycle management (Requested → Confirmed → Paid → Completed) |
| **Factory Method** | `backend/payment/` | Payment method creation (CreditCard, PayPal, BankTransfer, DebitCard) |
| **Proxy Pattern** | `backend/user/UserProxy.java` | Access control and role checking |
| **Strategy/Policy** | `backend/policy/` | Cancellation rules, pricing strategies (Fixed/Dynamic) |
| **Observer Pattern** | `backend/notification/` | Booking status notifications |
| **DAO Pattern** | `backend/database/` | Data access abstraction for all entities |

---

## 4. How to Run

### Option A: Docker (Recommended - Phase 2)

**Single command deployment:**

```bash
docker-compose up --build
```

This starts 4 containers:
- `eecs3311-db` - PostgreSQL database (port 5434)
- `eecs3311-app` - Backend API (port 8080)
- `eecs3311-frontend` - Web UI (port 3000)

**Access the application:**
- Frontend: http://localhost:3000
- API: http://localhost:8080/api
- Health check: http://localhost:8080/api/health

**Stop the application:**
```bash
docker-compose down
```

### Option B: Local Development (Phase 1)

**Prerequisites:**
- Java 17+
- PostgreSQL (optional, runs in demo mode without DB)

**Steps:**
1. Compile the project:
```bash
mkdir -p build/classes lib
wget -O lib/postgresql-42.6.0.jar https://jdbc.postgresql.org/download/postgresql-42.6.0.jar
javac -d build/classes -cp "src:lib/*" src/backend/**/*.java src/frontend/**/*.java
```

2. Run the API server:
```bash
java -cp "build/classes:lib/*" backend.Main
```


---

## 5. Project Structure

```
EECS3311Project/
├── src/
│   ├── backend/
│   │   ├── api/              # REST API handlers (ApiServer.java)
│   │   ├── booking/          # Booking domain + State pattern (8 files)
│   │   ├── core/             # Service layer orchestration (7 files)
│   │   ├── database/         # DAOs + DB connection (11 files)
│   │   ├── notification/     # Observer pattern (2 files)
│   │   ├── payment/          # Payment + Factory pattern (11 files)
│   │   ├── policy/           # Strategy/Policy patterns (9 files)
│   │   ├── service/          # Service entities (1 file)
│   │   ├── user/             # Users + Proxy pattern (5 files)
│   │   ├── EnvConfig.java    # Environment configuration
│   │   └── Main.java         # Application entry point
│   └── frontend/
│       ├── BookingUI.java    # Swing GUI frontend
│       └── FrontendEnvConfig.java  # Frontend configuration
├── diagrams/                 # UML diagrams
├── docker-compose.yml        # Docker orchestration
├── nginx.conf                # Frontend server config
├── database.properties       # DB configuration
├── .env.example              # Environment template
├── API_DOCUMENTATION.md      # Detailed API docs
├── AI_CHATBOT_DOCUMENTATION.md  # Chatbot documentation
├── DOCKER_README.md          # Docker setup guide
└── README.md                 # This file
```

### Package Responsibilities

| Package | Responsibility                                                  | Files |
|---------|-----------------------------------------------------------------|-------|
| `booking` | Booking lifecycle, state transitions                            | 8 |
| `core` | Use case orchestration (ClientService, ConsultantService, etc.) | 7 |
| `payment` | Payment creation, processing, transaction history               | 11 |
| `policy` | Business rules (cancellation, pricing, refunds)                 | 9 |
| `user` | User entities, roles, access control                            | 5 |
| `notification` | Simulated notifications (Observer pattern)                      | 2 |
| `service` | Consulting service catalog                                      | 1 |
| `database` | PostgreSQL DAOs and connection management                       | 11 |
| `api` | REST API endpoints                                              | 1 |
| `frontend` | Java cli interface                                              | 2 |


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
| POST | `/api/payments/methods/add` | Add payment method |
| GET | `/api/payments/methods` | Get user's payment methods |

### AI Chatbot
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/chat` | Chat with AI assistant |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/pending-consultants` | Get pending consultant approvals |
| POST | `/api/admin/approve-consultant` | Approve a consultant |
| POST | `/api/admin/set-policy` | Set system policy |

### Consultant Availability
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/consultants/availability` | Set availability |
| GET | `/api/consultants/availability` | Get availability |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |

---

## 7. Booking Lifecycle

```
Requested → Confirmed → PendingPayment → Paid → Completed
     ↓          ↓                           ↓
 Rejected   Cancelled                    Cancelled
```

### State Transitions

| From State | Action | To State | Actor |
|------------|--------|----------|-------|
| Requested | Confirm | Confirmed → PendingPayment | Consultant |
| Requested | Reject | Rejected | Consultant |
| PendingPayment | Pay | Paid | Client |
| PendingPayment | Cancel | Cancelled | Client |
| Paid | Complete | Completed | Consultant |
| Paid | Cancel | Cancelled + Refund | Client |

---

## 8. AI Chatbot Documentation

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

## 9. Team Contributions

| Member | Responsibilities | GitHub |
|--------|-----------------|--------|
| Bin Tang | Backend architecture, Docker setup, AI integration, frontend/backend functions | K1fight |
| Zehao Liu | Correction of backend and frontend, documentation | liu0205-mario |
| Haiyun He | UML diagrams, backend additions | 3canary |

---

## 10. Configuration

### Environment Variables (.env.example)


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
- CLI frontend only

### Phase 2
- AI chatbot uses LLM
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

## License

York University EECS 3311 Course Project - 2026

---

**Questions?** Check `API_DOCUMENTATION.md` or run `docker-compose logs` for debugging.
