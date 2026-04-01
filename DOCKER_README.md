# Docker Setup Guide

## Overview
This project uses Docker Compose to containerize both the frontend (React web UI) and backend (Java API server) with PostgreSQL database.

## Architecture
- **Frontend** (Port 3000): Nginx web server serving the HTML/CSS/JS interface
- **Backend** (Port 8080): Java API server handling business logic
- **Database** (Port 5434): PostgreSQL database for data persistence

## Quick Start

### 1. Build and Run All Services
```bash
docker-compose up --build
```

This will:
- Build the frontend image with nginx
- Start the backend Java application
- Start the PostgreSQL database
- Connect all services together

### 2. Access the Application
- **Frontend Web UI**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Database**: localhost:5434 (myuser/mypassword)

### 3. Stop All Services
```bash
docker-compose down
```

To also remove volumes (database data):
```bash
docker-compose down -v
```

## Service Details

### Frontend Service
- Serves static HTML, CSS, and JavaScript files
- Proxies API requests to the backend server
- Uses nginx for efficient static file serving

### Backend Service
- Compiles and runs the Java application
- Connects to PostgreSQL database
- Exposes REST API endpoints

### Database Service
- PostgreSQL 15
- Persistent storage with volume
- Health check configured

## Development

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f frontend
docker-compose logs -f app
docker-compose logs -f db
```

### Rebuild After Changes
```bash
# Rebuild and restart
docker-compose up --build
```

### Run Commands in Containers
```bash
# Backend container
docker exec -it eecs3311-app sh

# Frontend container
docker exec -it eecs3311-frontend sh

# Database container
docker exec -it eecs3311-db psql -U myuser -d mydb
```

## Troubleshooting

### Frontend Can't Connect to Backend
- Ensure backend is running: `docker-compose ps`
- Check backend logs: `docker-compose logs app`
- Verify API URL in browser console

### Database Connection Issues
- Wait for database health check to pass
- Check database logs: `docker-compose logs db`
- Verify connection string in backend code

### Port Already in Use
If ports 3000, 8080, or 5434 are in use, modify `docker-compose.yml`:
```yaml
ports:
  - "3001:80"    # Change frontend port
  - "8081:8080"  # Change backend port
  - "5435:5432"  # Change database port
```

## Production Deployment

For production, consider:
1. Using environment variables for configuration
2. Setting up proper SSL/TLS
3. Using Docker secrets for sensitive data
4. Implementing proper logging and monitoring
5. Setting up automatic backups for database

## Notes
- The frontend uses nginx to serve static files and proxy API requests
- The backend compiles Java code on startup (for development convenience)
- Database data persists between restarts using Docker volumes
- All services are configured to restart automatically unless stopped
