# EECS 3311 Project (Phase 1)
## Service Booking & Consulting Platform

## Github URL
https://github.com/K1fight/EECS3311Project.git

## 1. What this project is
This project is a **service booking and consulting platform**.  
A **Client** can request a booking for a consulting service. A **Consultant** can accept or reject the request, and later complete the booking. The system also supports **simulated payments**, **cancellation rules**, and **basic access control**.

Phase 1 focuses on:
- clean backend structure (OOP design),
- using design patterns in a meaningful way,
- and providing a minimal UI to demonstrate the workflow.

---

## 2. What we can do (Phase 1 features)

### Client
- Browse services
- Request a booking
- Cancel a booking
- Make a payment (simulated)
- View booking/payment history (in-memory demo level)

### Consultant
- Accept a booking
- Reject a booking
- Complete a booking
- Manage availability (simplified/in-memory)

### Admin
- Manage system rules (policy framework exists; some policy logic is minimal/placeholder in Phase 1 and designed to be extended)

---

## 3. How to run
### 3.1 Run the CLI frontend
Our Phase 1 frontend is a **terminal UI**.

**Entry file**
- `BookingUI.java`

**Steps**
1. Open the project in an IDE (IntelliJ / Eclipse / VSCode).
2. Run `BookingUI.main()`.
3. Use the menu to simulate an end-to-end flow:
   - Client: browse services → request booking  
   - Consultant: accept booking  
   - Client: pay  
   - Consultant: complete booking  

> This UI is intentionally simple. It collects input and calls backend services. All business logic is in the backend.

---

## 4. Project structure (where things are)
backend/
booking/        # Booking domain + lifecycle (State Pattern)
core/           # Service layer: coordinates use cases
payment/        # Payment creation/processing + transaction history (Factory Method)
policy/         # Cancellation/refund/pricing rules (policy/strategy-style)
user/           # Users + access control (Proxy)
notification/   # Simulated notification sending
service/        # Consulting service entities/catalog
frontend/
BookingUI.java  # CLI UI to drive the workflows
### Why we separated packages like this
We did this to keep responsibilities clear:
- `booking` focuses on the booking lifecycle rules
- `core` focuses on use case flow (the steps of each user action)
- `payment` focuses on payment creation and recording transactions
- `policy` focuses on business rules that may change (refund/cancellation/pricing)
- `user` focuses on roles and access control logic
- `notification` focuses on messaging (simulated)
- `service` holds the consulting service definitions

This keeps the code easier to understand and easier to extend.

---

## 5. Core workflow (end-to-end scenario)
A typical scenario works like this:
1. **Client** requests a booking → booking starts in **Requested** state
2. **Consultant** accepts or rejects
3. If accepted, **Client** pays → booking moves to **Paid**
4. **Consultant** completes the booking → booking moves to **Completed**
5. Client may cancel depending on cancellation policy and booking state

---

## 6. Design patterns used (where + why)

### 6.1 State Pattern (Booking lifecycle)
**Why**

A booking behaves differently depending on its state.  
If we used only an enum and many `if/switch` statements, the booking logic would become long and error-prone.

**How it works in our code**
- `Booking` holds a `BookingState currentState`
- Booking actions delegate to the state object
- Each state class decides whether an action is allowed, and performs state transitions

**Main files**
- `backend/booking/Booking.java`
- `backend/booking/BookingState.java`
- Concrete states:
  - `RequestedState.java`
  - `ConfirmedState.java`
  - `PendingPaymentState.java`
  - `PaidState.java`
  - `CancelledState.java`
  - `RejectedState.java`
  - `CompletedState.java`

**What you should see**
- Methods like `confirm() / cancel() / reject() / markPaid() / complete()` are allowed or blocked depending on the current state.
- State transitions happen inside concrete state classes.

---

### 6.2 Factory Method (Payment creation)
**Why**

We support multiple payment methods. We do not want the service layer to directly depend on concrete classes like `new CreditCardPayment()` everywhere.

**How it works in our code**
- A factory creates the correct payment object based on the selected payment type
- Payment processing uses the factory output instead of directly instantiating concrete classes

**Main files**
- Factory:
  - `backend/payment/PaymentFactory.java`
- Concrete factories / implementations under:
  - `backend/payment/creditCard/`
  - `backend/payment/debitCard/`
  - `backend/payment/paypal/`
  - `backend/payment/bankTransfer/`
- Orchestration:
  - `backend/core/PaymentService.java`

**What you should see**
- Adding a new payment method is mainly “add a new payment class + factory”, with minimal changes elsewhere.

---

### 6.3 Proxy Pattern (Access control)
**Why**

Role checking and login checking can become duplicated if written in every service method.  
We centralize access checks in one place.

**How it works in our code**
- `UserProxy` checks login status and user role before delegating operations to the real user object

**Main files**
- `backend/user/UserProxy.java`
- `backend/user/User.java`
- `backend/user/Client.java`
- `backend/user/Consultant.java`
- `backend/user/Admin.java`

---

### 6.4 Policy / Strategy-style design (Cancellation / Refund / Pricing rules)
**Why**

Business rules change. We do not want to hard-code rules inside booking lifecycle logic.

**How it works in our code**
- Policy interfaces represent rules
- Implementations represent specific rule sets (default rules now, easy to extend later)

**Main files**
- `backend/policy/CancellationPolicy.java`
- `backend/policy/DefaultCancellationPolicy.java`
- Other policy interfaces/classes exist as extension points for Phase 2

---

## 7. Service layer (where use cases are executed)
Use cases are orchestrated in `backend/core/`:
- `ClientService.java` — client workflows (request/cancel/browse/history)
- `ConsultantService.java` — accept/reject/complete workflows
- `PaymentService.java` — payment workflow + transaction + booking state update
- `AdminService.java` — admin workflows (policy configuration; some parts may be minimal in Phase 1)
- `BookingService.java` — booking management (in-memory demo style)

The idea is:
- **core** coordinates steps (use case flow)
- **booking/payment/policy/user** implement domain rules and modules

---

## 8. Data storage
Phase 1 uses **in-memory storage** (lists/maps).  
There is no database persistence in this phase.

---

## 9. Notes / known limitations (Phase 1)
- The UI is CLI-based (minimal frontend).
- Some admin policy configuration is implemented as a framework/extension point rather than a full production system.
- Payment and notification are simulated (no real external APIs).

---

## 10. How to demonstrate quickly (for evaluation)
A quick demo path:
1. Run `BookingUI`
2. Login as Client → browse services → request booking
3. Login as Consultant → accept booking
4. Login as Client → pay
5. Login as Consultant → complete booking

---

## 11. Team contribution (fill in)
Member A:  Bin Tang
- Responsible for backend
- github name: K1fight


Member B:  Zehao Liu
- Responsible for correction of backend and frontend and documentation
- github name: liu0205-mario


Member C:  Haiyun He
- Responsible for UML diagrams and additions to the backend
- github name: 3canary
