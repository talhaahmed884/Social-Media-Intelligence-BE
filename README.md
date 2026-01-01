# Social Media Intelligence

A Spring Boot application designed to collect and analyze social media data from various platforms, starting with robust
user management and authentication capabilities.

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Security Features](#security-features)
- [Development](#development)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Overview

Social Media Intelligence is a comprehensive Spring Boot application built to aggregate and analyze social media data.
The current implementation focuses on a robust user management system with secure authentication, serving as the
foundation for future social media integration features.

**Base Package:** `com.media.intelligence`

---

## Features

### ✅ User Management

- **User Registration** - Create new user accounts with email validation
- **User Profile Management** - Update user information (email, full name)
- **User Retrieval** - Fetch users by ID, email, or get all users
- **User Deletion** - Remove user accounts with cascade delete

### ✅ Authentication & Security

- **Password Management** - Secure password hashing with BCrypt
- **Password Change** - Change password with current password verification
- **Strong Password Validation** - Enforces complexity requirements
- **XSS Protection** - Input sanitization to prevent cross-site scripting

### ✅ Data Validation & Sanitization

- **Email Validation** - RFC 5322 compliant email format validation
- **Email Normalization** - Lowercase conversion, Gmail dot removal
- **Input Sanitization** - Whitespace trimming, XSS protection
- **Custom Validators** - Field-level and cross-field validation

### ✅ API & Integration

- **RESTful API** - Well-structured REST endpoints
- **Standardized Responses** - Consistent API response format
- **Postman Collection** - Complete API testing collection
- **Error Handling** - Domain-specific exceptions with meaningful messages

### ✅ Database

- **PostgreSQL** - Production database with UUID primary keys
- **H2 Database** - In-memory database for testing
- **JPA/Hibernate** - ORM with entity relationships
- **Shared Primary Key** - One-to-one relationship using @MapsId

### ✅ Testing

- **Unit Tests** - Service and component layer tests with Mockito
- **Integration Tests** - Repository and end-to-end tests
- **Test Coverage** - Comprehensive test suites for all layers
- **H2 Test Database** - PostgreSQL-compatible test environment

---

## Technology Stack

### Core Framework

- **Java 25** - Latest Java LTS features
- **Spring Boot 4.0.1** - Application framework
- **Maven** - Dependency management and build tool

### Database

- **PostgreSQL** - Production relational database
- **H2 Database** - In-memory database for testing
- **Spring Data JPA** - Data access layer
- **Hibernate** - ORM framework

### Security & Validation

- **BCrypt** - Password hashing algorithm
- **Bean Validation** - JSR-303 validation
- **Custom Validators** - Domain-specific validation logic
- **XSS Protection** - Input sanitization

### Development Tools

- **Lombok** - Reduce boilerplate code
- **Spring DevTools** - Hot reload and development utilities
- **Spring Boot Docker Compose** - Container support
- **Logback** - Logging framework

### Testing

- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing support
- **Testcontainers** - Ready for container-based testing

---

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│          Controller Layer               │
│  (REST endpoints, request handling)     │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│          Service Layer                  │
│  (Business logic, validation,           │
│   sanitization, orchestration)          │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│          Repository Layer               │
│  (Data access, JPA queries)             │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│          Database Layer                 │
│  (PostgreSQL / H2)                      │
└─────────────────────────────────────────┘
```

### Domain Structure

Following **Vertical Slice Architecture**, each domain is self-contained:

```
user/
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # Data access
├── entity/         # JPA entities
├── dto/           # Data transfer objects
├── validation/    # Validators
├── sanitization/  # Input sanitizers
└── exception/     # Domain exceptions

user_credential/
├── service/
├── repository/
├── entity/
├── exception/
└── strategy/      # Password hashing strategy
```

### Key Design Patterns

- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic separation
- **DTO Pattern** - Data transfer and validation
- **Strategy Pattern** - Pluggable password hashing
- **Builder Pattern** - Entity construction
- **Exception Translation** - Domain-specific exceptions

---

## Getting Started

### Prerequisites

- **Java 25** or higher
- **Maven 3.6+**
- **PostgreSQL 14+** (for production)
- **Git**
- **Postman** (optional, for API testing)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/Social-Media-Intelligence.git
   cd Social-Media-Intelligence
   ```

2. **Configure Database**

   Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/social_media_intelligence
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

### Running the Application

**Development Mode:**

```bash
mvn spring-boot:run
```

**Production Mode:**

```bash
mvn package
java -jar target/social-media-intelligence-0.0.1-SNAPSHOT.jar
```

**Access the application:**

- API Base URL: `http://localhost:8080/api/v1`
- H2 Console (test profile): `http://localhost:8080/h2-console`

---

## API Documentation

### Base URL

```
http://localhost:8080/api/v1
```

### Endpoints

#### User Management

| Method | Endpoint                          | Description         | Auth Required |
|--------|-----------------------------------|---------------------|---------------|
| POST   | `/users/register`                 | Register new user   | No            |
| GET    | `/users/{userId}`                 | Get user by ID      | Future        |
| GET    | `/users/by-email?email={email}`   | Get user by email   | Future        |
| GET    | `/users`                          | Get all users       | Future        |
| PUT    | `/users/{userId}`                 | Update user profile | Future        |
| PUT    | `/users/{userId}/change-password` | Change password     | Future        |
| DELETE | `/users/{userId}`                 | Delete user         | Future        |

### Request/Response Examples

#### Register User

**Request:**

```http
POST /api/v1/users/register
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
}
```

**Response (201 Created):**

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "createdAt": "2025-12-31T10:00:00",
    "updatedAt": "2025-12-31T10:00:00"
  },
  "timestamp": "2025-12-31T10:00:00"
}
```

#### Error Response

**Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Email already exists",
  "data": null,
  "timestamp": "2025-12-31T10:00:00"
}
```

### Postman Collection

Import the included Postman collection for easy API testing:

- Collection: `Social_Media_Intelligence_API.postman_collection.json`
- Environment: `Social_Media_Intelligence.postman_environment.json`

See [POSTMAN_GUIDE.md](POSTMAN_GUIDE.md) for detailed instructions.

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────────┐
│        users            │
├─────────────────────────┤
│ id (PK)        UUID     │
│ email          VARCHAR  │
│ full_name      VARCHAR  │
│ created_at     TIMESTAMP│
│ updated_at     TIMESTAMP│
└───────────┬─────────────┘
            │
            │ 1:1 (shared PK)
            │
┌───────────▼─────────────┐
│   user_credentials      │
├─────────────────────────┤
│ id (PK,FK)     UUID     │
│ password_hash  VARCHAR  │
│ created_at     TIMESTAMP│
│ updated_at     TIMESTAMP│
└─────────────────────────┘
```

### Key Relationships

- **User ↔ UserCredential**: One-to-one relationship using `@MapsId`
    - Shared primary key design
    - Foreign key constraint: `fk_user_credential_user`
    - Cascade delete: Deleting user removes credentials

### Database Features

- **UUID Primary Keys** - Distributed system ready
- **Indexed Email** - Fast email lookups with unique constraint
- **Timestamps** - Automatic created_at and updated_at
- **Foreign Key Constraints** - Referential integrity
- **Cascade Operations** - Automatic cleanup

---

## Testing

### Running Tests

**All Tests:**

```bash
mvn test
```

**Specific Test Class:**

```bash
mvn test -Dtest=UserServiceTest
```

**Integration Tests Only:**

```bash
mvn test -Dtest=*IntegrationTest
```

### Test Coverage

- ✅ **Unit Tests** - Service layer, validators, sanitizers
- ✅ **Integration Tests** - Repository layer, service integration
- ✅ **MockMVC Tests** - Controller endpoints (ready to add)
- ✅ **H2 Test Database** - PostgreSQL-compatible test environment

### Test Configuration

Tests use H2 in-memory database with PostgreSQL compatibility mode:

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=SET REFERENTIAL_INTEGRITY TRUE
```

---

## Project Structure

```
social-media-intelligence/
├── src/
│   ├── main/
│   │   ├── java/com/media/intelligence/
│   │   │   ├── common/              # Shared components
│   │   │   │   ├── dto/            # Common DTOs (ApiResponse)
│   │   │   │   ├── validation/     # Validation framework
│   │   │   │   └── sanitization/   # Sanitization framework
│   │   │   ├── user/               # User domain
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   ├── validation/
│   │   │   │   ├── sanitization/
│   │   │   │   └── exception/
│   │   │   ├── user_credential/    # User credential domain
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── strategy/
│   │   │   │   └── exception/
│   │   │   └── SocialMediaIntelligenceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
│       ├── java/com/media/intelligence/
│       │   ├── common/             # Common component tests
│       │   ├── user/               # User tests
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   └── validation/
│       │   └── user_credential/    # Credential tests
│       │       ├── service/
│       │       └── repository/
│       └── resources/
│           └── application-test.yml
├── logs/                           # Application logs
├── pom.xml                         # Maven configuration
├── README.md                       # This file
├── POSTMAN_GUIDE.md               # Postman usage guide
├── Social_Media_Intelligence_API.postman_collection.json
└── Social_Media_Intelligence.postman_environment.json
```

---

## Security Features

### Password Security

- **BCrypt Hashing** - Industry-standard password hashing
- **Salt Generation** - Automatic per-password salt
- **Strength Validation** - Enforces strong password requirements:
    - Minimum 8 characters
    - At least one uppercase letter
    - At least one lowercase letter
    - At least one digit
    - At least one special character

### Input Validation

- **Email Validation** - RFC 5322 compliant
- **Length Validation** - Field length constraints
- **Required Fields** - Null/empty checks
- **Custom Validators** - Domain-specific rules

### Input Sanitization

- **XSS Protection** - HTML entity encoding
- **Whitespace Trimming** - Remove leading/trailing spaces
- **Email Normalization** - Lowercase conversion, Gmail dot removal
- **SQL Injection Prevention** - JPA parameterized queries

### Database Security

- **Foreign Key Constraints** - Data integrity
- **Unique Constraints** - Prevent duplicates
- **Not Null Constraints** - Required field enforcement
- **Cascade Delete** - Cleanup orphaned records

---

## Development

### Code Style

- Follow **Java naming conventions**
- Use **Lombok** annotations to reduce boilerplate
- **Single Responsibility Principle** per class
- **Vertical Slice Architecture** per domain

### Logging

- **SLF4J + Logback** for logging
- Log levels: DEBUG (development), INFO (production)
- Log file: `logs/application.log`
- Rolling policy: 10MB max, 30 days retention

### Environment Profiles

**Development:**

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

**Testing:**

```bash
mvn test -Dspring.profiles.active=test
```

**Production:**

```bash
java -jar app.jar --spring.profiles.active=prod
```

### Building for Production

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run JAR
java -jar target/social-media-intelligence-0.0.1-SNAPSHOT.jar
```

---

## Roadmap

### Phase 1: Foundation ✅

- [x] User management CRUD
- [x] Password authentication
- [x] Input validation & sanitization
- [x] RESTful API
- [x] Database schema
- [x] Unit & integration tests
- [x] Postman collection

### Phase 2: Authentication (In Progress)

- [ ] JWT token-based authentication
- [ ] Login/logout endpoints
- [ ] Session management
- [ ] Password reset flow
- [ ] Email verification

### Phase 3: Social Media Integration (Planned)

- [ ] Instagram connector
- [ ] Twitter/X connector
- [ ] Facebook connector
- [ ] LinkedIn connector
- [ ] Data collection scheduling

### Phase 4: Analytics (Planned)

- [ ] Data aggregation
- [ ] Sentiment analysis
- [ ] Engagement metrics
- [ ] Reporting dashboard
- [ ] Export functionality

### Phase 5: Advanced Features (Future)

- [ ] Multi-tenancy support
- [ ] API rate limiting
- [ ] Caching layer (Redis)
- [ ] Message queue (RabbitMQ/Kafka)
- [ ] Microservices architecture

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit changes** (`git commit -m 'Add amazing feature'`)
4. **Push to branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Coding Standards

- Follow existing code style
- Add unit tests for new features
- Update documentation
- Use meaningful commit messages

### Testing Requirements

- All new features must include tests
- Maintain or improve test coverage
- All tests must pass before PR

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contact & Support

- **Project Repository**: [GitHub](https://github.com/yourusername/Social-Media-Intelligence)
- **Issues**: [GitHub Issues](https://github.com/yourusername/Social-Media-Intelligence/issues)

---

## Acknowledgments

- **Spring Boot Team** - Excellent framework and documentation
- **Hibernate Team** - Robust ORM implementation
- **Community Contributors** - Open source dependencies

---

**Built with ❤️ using Spring Boot 4.0.1 and Java 25**
