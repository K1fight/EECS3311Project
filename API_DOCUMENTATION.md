# Consulting Booking System - REST API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
Currently, all endpoints are open for development. Authentication will be added in future versions.

---

## Endpoints

### Health Check

#### GET /health
Check if the API server is running.

**Response:**
```json
{
  "status": "ok",
  "message": "API server is running"
}
```

---

### User Management

#### POST /users/register
Register a new user (Client, Consultant, or Admin).

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "accountType": "Client"
}
```

**Response:**
```json
{
  "success": true,
  "userId": "uuid-here",
  "message": "User registered successfully"
}
```

#### POST /users/login
Login user.

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "user": {
    "userId": "uuid-here",
    "name": "John Doe",
    "email": "john@example.com",
    "accountType": "Client"
  }
}
```

#### GET /users/profile
Get user profile.

**Query Parameters:**
- `userId` - User ID

**Response:**
```json
{
  "success": true,
  "user": {
    "userId": "uuid-here",
    "name": "John Doe",
    "email": "john@example.com",
    "accountType": "Client"
  }
}
```

#### GET /users/consultants
Get all consultants.

**Response:**
```json
{
  "success": true,
  "consultants": [
    {
      "userId": "uuid-here",
      "name": "Jane Smith",
      "email": "jane@example.com",
      "isApproved": true
    }
  ]
}
```

#### GET /users/clients
Get all clients.

**Response:**
```json
{
  "success": true,
  "clients": [
    {
      "userId": "uuid-here",
      "name": "John Doe",
      "email": "john@example.com"
    }
  ]
}
```

#### POST /users/approve-consultant
Approve a consultant account (Admin only).

**Request Body:**
```json
{
  "consultantId": "uuid-here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Consultant approved successfully"
}
```

---

### Service Management

#### GET /services
Get all available consulting services.

**Response:**
```json
{
  "success": true,
  "services": [
    {
      "serviceId": "uuid-here",
      "name": "Business Consulting",
      "description": "Professional business advice",
      "basePrice": 100.00,
      "durationMinutes": 60,
      "category": "BUSINESS"
    }
  ]
}
```

#### POST /services/create
Create a new consulting service.

**Request Body:**
```json
{
  "name": "Business Consulting",
  "description": "Professional business advice",
  "basePrice": 100.00,
  "durationMinutes": 60,
  "category": "BUSINESS"
}
```

**Response:**
```json
{
  "success": true,
  "serviceId": "uuid-here",
  "message": "Service created successfully"
}
```

---

### Booking Management

#### POST /bookings/create
Create a new booking.

**Request Body:**
```json
{
  "clientId": "uuid-here",
  "consultantId": "uuid-here",
  "serviceId": "uuid-here",
  "startTime": "2026-03-30T10:00:00"
}
```

**Response:**
```json
{
  "success": true,
  "bookingId": "uuid-here",
  "status": "Requested",
  "message": "Booking created successfully"
}
```

#### GET /bookings/client
Get all bookings for a client.

**Query Parameters:**
- `clientId` - Client ID

**Response:**
```json
{
  "success": true,
  "bookings": [
    {
      "bookingId": "uuid-here",
      "clientName": "John Doe",
      "consultantName": "Jane Smith",
      "serviceName": "Business Consulting",
      "startTime": "2026-03-30T10:00:00",
      "status": "Confirmed"
    }
  ]
}
```

#### GET /bookings/consultant
Get all bookings for a consultant.

**Query Parameters:**
- `consultantId` - Consultant ID

**Response:**
```json
{
  "success": true,
  "bookings": [
    {
      "bookingId": "uuid-here",
      "clientName": "John Doe",
      "serviceName": "Business Consulting",
      "startTime": "2026-03-30T10:00:00",
      "status": "Confirmed"
    }
  ]
}
```

#### POST /bookings/confirm
Confirm a booking (Consultant only).

**Request Body:**
```json
{
  "bookingId": "uuid-here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking confirmed successfully"
}
```

#### POST /bookings/cancel
Cancel a booking.

**Request Body:**
```json
{
  "bookingId": "uuid-here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking cancelled successfully"
}
```

#### POST /bookings/complete
Complete a booking.

**Request Body:**
```json
{
  "bookingId": "uuid-here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking completed successfully"
}
```

---

### Payment Management

#### POST /payments/pay
Make a payment for a booking.

**Request Body:**
```json
{
  "bookingId": "uuid-here",
  "paymentMethod": "CreditCard",
  "amount": 100.00
}
```

**Response:**
```json
{
  "success": true,
  "paymentId": "uuid-here",
  "status": "SUCCESS",
  "message": "Payment processed successfully"
}
```

#### GET /payments/history
Get payment history.

**Query Parameters:**
- `userId` - User ID

**Response:**
```json
{
  "success": true,
  "payments": [
    {
      "paymentId": "uuid-here",
      "bookingId": "uuid-here",
      "amount": 100.00,
      "paymentMethod": "CreditCard",
      "status": "SUCCESS",
      "timestamp": "2026-03-30T10:00:00"
    }
  ]
}
```

---

## Error Responses

All endpoints may return error responses in the following format:

```json
{
  "success": false,
  "error": "Error message here"
}
```

### Common HTTP Status Codes
- `200 OK` - Request successful
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## CORS Support

All endpoints support Cross-Origin Resource Sharing (CORS) for frontend integration.

**Headers:**
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type`

---

## Running the Server

### From IntelliJ IDEA
Run the `backend.Main` class directly.

### From Command Line
```bash
javac -d build src/backend/Main.java src/backend/api/*.java ...
java -cp build backend.Main
```

### Using Docker
```bash
docker-compose up --build
```

The server will start on port **8080**.
