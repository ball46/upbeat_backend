# UPBEAT Backend

Backend system for UPBEAT platform, providing user authentication and game management.

## 📋 Project Overview

UPBEAT is a turn-based game where players attempt to claim territories. The backend manages user authentication, game mechanics, and API endpoints for client interaction.

## 🛠️ Technologies Used

- Java 21
- Spring Boot 3.4.3
- Spring Security
- PostgreSQL
- Redis
- JPA (Hibernate)
- Bean Validation (Jakarta Validation)
- JUnit 5 & Mockito

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/upbeat_backend/
│   │       ├── config/          # Configuration classes and security setup
│   │       ├── controller/      # REST API endpoints
│   │       ├── dto/             # Data transfer objects
│   │       │   └── request/     # Request DTOs
│   │       │   └── response/    # Response DTOs
│   │       ├── exception/       # Custom exceptions
│   │       │   ├── base/        # Base exception classes
│   │       │   ├── handler/     # Global exception handling
│   │       │   └──response/     # Exception response handling
│   │       ├── model/           # Data models
│   │       │   └── enums/       # Enumerations
│   │       ├── repository/      # Data access layer
│   │       ├── security/        # Security classes (UserPrincipal)
│   │       │   └── jwt/         # JWT utilities
│   │       │   └── permission/  # Permission management
│   │       │   └── service/     # Security services
│   │       ├── service/         # Business logic
│   │       ├── util/            # Utility classes
│   │       ├── validation/      # Custom validators
│   │       │   ├── annotation/  # Custom validation annotations
│   │       │   └── validator/   # Custom validator implementations
│   │       └── UpbeatBackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/example/upbeat_backend/
            ├── controller/      # Controller tests
            ├── security/        # Security tests
            └── service/         # Service tests
```

## 🚀 Installation

1. Clone the repository:
```bash
git clone https://github.com/ball46/upbeat-backend.git
cd upbeat-backend
```

2. Build the project with Maven:
```bash
./mvnw clean install
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

## 💡 Key Features

### User Authentication and Management
- Secure login and registration
- JWT-based authentication
- Password management with secure hashing
- Custom validation for password requirements

### Game State Management
- Creation and management of game sessions
- Player and resource management
- Turn-based gameplay handling

### RESTful API
- User registration and authentication endpoints
- Game creation and management endpoints
- Player action submission

## 📝 API Usage

### Authentication
```
POST /auth/login
POST /auth/register
```

### User Management
```
POST /users/change-password
```
Example request body:
```json
{
  "oldPassword": "OldPass123*",
  "newPassword": "NewPass123*"
}
```

### Game Management
```
GET /games
POST /games
GET /games/{gameId}
```

## 🧪 Testing

Run unit tests:
```bash
./mvnw test
```

## 🔄 Current Status & Future Plans

- User authentication system implemented
- Password management functionality complete
- API endpoints for user operations available
- Game mechanics in development
- Planned: Extended game functionality and multiplayer support

## 👨‍💻 Developers

- UPBEAT Development Team