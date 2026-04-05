# Database Module Documentation

## Overview
This database module provides a complete data persistence layer for the Consulting Booking System using SQLite (with support for MySQL and PostgreSQL).

## Features
- ✅ Singleton database connection management
- ✅ Support for multiple database systems (SQLite, MySQL, PostgreSQL)
- ✅ Base DAO class with common CRUD operations
- ✅ Type-safe DAO implementations
- ✅ Automatic schema initialization
- ✅ Foreign key constraint support
- ✅ Prepared statements for SQL injection prevention

## Module Structure

```
src/backend/database/
├── DatabaseConnection.java      # Singleton connection manager
├── BaseDAO.java                 # Base class with common DB operations
├── DatabaseInitializer.java     # Schema creation and migration
├── UserDAO.java                 # User data access operations
├── BookingDAO.java              # Booking data access operations
├── DatabaseExample.java         # Usage examples
└── README.md                    # This file
```

## Setup Instructions

### 1. Add JDBC Dependencies

**For SQLite:**
Download SQLite JDBC driver and add to your project:
- Maven: `org.xerial:sqlite-jdbc:3.42.0.0`
- Or download JAR from: https://github.com/xerial/sqlite-jdbc/releases

**For MySQL:**
- Maven: `mysql:mysql-connector-java:8.0.33`

**For PostgreSQL:**
- Maven: `org.postgresql:postgresql:42.6.0`

### 2. Configure Database

Edit `database.properties` file to select your database:

**SQLite (Recommended for Development):**
```properties
db.type=sqlite
db.url=jdbc:sqlite:consulting_booking.db
```

**MySQL (Production):**
```properties
db.type=mysql
db.url=jdbc:mysql://localhost:3306/consulting_booking?useSSL=false
db.username=root
db.password=your_password
```

### 3. Initialize Database

```java
// In your application startup code
DatabaseConnection db = DatabaseConnection.getInstance();
db.connect();

DatabaseInitializer initializer = new DatabaseInitializer();
initializer.initialize(); // Creates all tables
```

## Usage Examples

### Basic Operations

```java
// Get database connection
DatabaseConnection db = DatabaseConnection.getInstance();
db.connect();

// Create DAO instances
UserDAO userDAO = new UserDAO();
BookingDAO bookingDAO = new BookingDAO();
```

### User Operations

```java
// Create a new client
Client client = new Client("John Doe", "john@example.com", "password123");
userDAO.insert(client);

// Find user by email
User user = userDAO.findByEmail("john@example.com");

// Get all clients
List<Client> clients = userDAO.getAllClients();

// Get pending consultants
List<Consultant> pending = userDAO.getPendingConsultants();

// Approve consultant
userDAO.updateApprovalStatus(consultantId, true);
```

### Booking Operations

```java
// Create a booking
Booking booking = new Booking(client, consultant, service, startTime);
bookingDAO.insert(booking);

// Get bookings by client
List<Booking> clientBookings = bookingDAO.findByClientId(clientId);

// Get bookings by consultant
List<Booking> consultantBookings = bookingDAO.findByConsultantId(consultantId);

// Update booking status
booking.setStatus(BookingStatus.Confirmed);
bookingDAO.update(booking);
```

## Database Schema

### Tables Created

1. **users** - Stores all user accounts (Admin, Client, Consultant)
2. **consulting_services** - Service catalog
3. **bookings** - Booking records with state tracking
4. **payments** - Payment transactions
5. **payment_methods** - Saved payment methods for clients
6. **consultant_availability** - Consultant availability schedules

### Schema Diagram

```
users (user_id PK, name, email, password, account_type, is_approved)
  │
  ├─< bookings (client_id FK, consultant_id FK)
  │
  └─< payment_methods (client_id FK)

consulting_services (service_id PK)
  │
  └─< bookings (service_id FK)

bookings (booking_id PK, status, current_state)
  │
  └─< payments (booking_id FK)
```

## Best Practices

1. **Always close connections**: Use try-with-resources or finally blocks
2. **Use DAOs**: Don't execute raw SQL in business logic
3. **Transaction management**: Group related operations in transactions
4. **Error handling**: Catch and log SQLExceptions appropriately
5. **Connection pooling**: Consider using a connection pool for production

## Extending the Module

### Create a New DAO

```java
public class PaymentDAO extends BaseDAO {
    
    public boolean insert(Payment payment) {
        String sql = "INSERT INTO payments ...";
        return executeUpdate(sql, params) > 0;
    }
    
    public Payment findById(String id) {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";
        try (ResultSet rs = executeQuery(sql, id)) {
            if (rs.next()) {
                return mapToPayment(rs);
            }
        }
        return null;
    }
}
```

### Add Custom Queries

```java
public List<Booking> findRecentBookings(String clientId, int limit) {
    String sql = """
        SELECT * FROM bookings 
        WHERE client_id = ? 
        ORDER BY created_at DESC 
        LIMIT ?
        """;
    
    List<Booking> bookings = new ArrayList<>();
    try (ResultSet rs = executeQuery(sql, clientId, limit)) {
        while (rs.next()) {
            bookings.add(mapToBooking(rs));
        }
    }
    return bookings;
}
```

## Troubleshooting

### Common Issues

**"SQLite JDBC driver not found"**
- Ensure sqlite-jdbc JAR is in your classpath

**"Table doesn't exist"**
- Run `DatabaseInitializer.initialize()` to create tables

**"Foreign key constraint failed"**
- Ensure parent records exist before inserting child records
- Check that foreign keys are enabled: `PRAGMA foreign_keys = ON`

**"Database is locked" (SQLite)**
- Close connections properly after use
- Avoid concurrent writes without proper transaction management

## Performance Tips

1. **Use indexes**: Add indexes on frequently queried columns
2. **Batch operations**: Use batch inserts/updates for bulk operations
3. **Prepared statements**: Reuse prepared statements when possible
4. **Connection pooling**: Use a connection pool in production
5. **Query optimization**: Use EXPLAIN QUERY PLAN to optimize slow queries

## Security Considerations

1. ✅ **SQL Injection Prevention**: All queries use prepared statements
2. ⚠️ **Password Storage**: Currently stores plain text - implement hashing (BCrypt)
3. ⚠️ **Connection Security**: Use SSL/TLS for production databases
4. ⚠️ **Access Control**: Implement role-based database permissions

## Next Steps

- [ ] Add transaction support for multi-step operations
- [ ] Implement connection pooling (HikariCP)
- [ ] Add password hashing (BCrypt)
- [ ] Create migration scripts for schema updates
- [ ] Add unit tests for all DAOs
- [ ] Implement caching layer for frequently accessed data

## Support

For issues or questions, please refer to:
- JavaDocs in each class
- DatabaseExample.java for working examples
- SQLite documentation: https://www.sqlite.org/docs.html
