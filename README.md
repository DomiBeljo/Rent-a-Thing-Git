# Rent-a-Thing

A full-stack rental marketplace application that allows users to list items for rent, browse available listings, book rentals, and communicate through an integrated messaging system.


## Architecture

![Architecture](architecture.png)

The backend follows a layered architecture where controllers handle incoming HTTP requests, services contain the business logic, and repositories manage data persistence through Supabase PostgreSQL.

The backend communicates with the Rent-a-Thing mobile frontend built with React Native and Expo Go through secure REST APIs using JWT authentication.

React Native (Expo Go)
↕
REST API (JWT)
↕
Controllers → Services → Repositories → Supabase (PostgreSQL)

The backend is containerized using Docker, allowing consistent development and deployment environments across different platforms.

## Features

- JWT Authentication & Authorization
- Email Verification
- Listing & Item Management
- Booking System
- User Favorites
- Chat/Messaging
- Image Uploads
- REST API Architecture


### Backend
- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Supabase(PostgreSQL)
- JWT
- Docker

## Getting Started

### Prerequisites

- Java 25
- PostgreSQL
- Gradle

---

## Environment Configuration

Create a `.env` file in the project root.

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/rentathing
DB_USERNAME=postgres
DB_PASSWORD=password

JWT_SECRET=your_jwt_secret

MAIL_USERNAME=your_email
MAIL_PASSWORD=your_email_password
```

You may also configure these values inside:

```properties
application.properties
```

depending on your deployment setup.

---

## Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE rentathing;
```

Update your database credentials in the application configuration.

---


### Testing
- JUnit 5
- Mockito
- Cucumber
- JaCoCo
- (./gradlew test,
  ./gradlew check)

The application follows a layered architecture:

### Future Improvements
- Real-time chat using WebSockets
- Ratings & Reviews
- Payment Integration
- Push Notifications

```text
Controllers → Services → Repositories → Supabase(PostgreSQL)

