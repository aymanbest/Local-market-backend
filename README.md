# Local Market API

## Overview
Local Market is a platform connecting local producers with customers, enabling direct purchase of locally produced goods. This repository contains the backend API built with Spring Boot.

## Features
- User authentication and authorization (Customer, Producer, Admin roles)
- Product management with categories
- Order processing and management
- Producer application system
- Review and rating system
- Payment processing
- Coupon and discount management
- Real-time notifications via WebSockets
- Analytics for sales and user activity
- Support ticket system
- Region-based filtering

## Technology Stack
- Java 17
- Spring Boot 3.4.1
- Spring Security with JWT authentication
- Spring Data JPA
- MySQL Database
- Swagger/OpenAPI for API documentation
- WebSockets for real-time communication
- Cloudinary for image storage

## Getting Started

### Prerequisites
- Java 17 or higher
- MySQL
- Maven

### Setup
1. Clone the repository:
   ```
   git clone https://github.com/aymanbest/Local-market-backend.git
   ```

2. Configure environment variables:
   - Copy `.env.example` to `.env` and update the values
   - Required variables include database connection, JWT secret, admin credentials, and Cloudinary settings

3. Build the project:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn spring-boot:run
   ```

### Database Setup
The database schema is provided in `database.sql`. You can import this file to set up your database structure.

## API Documentation
The API is documented using Swagger/OpenAPI. Once the application is running, you can access the documentation at:

```
http://localhost:8080/swagger-ui/index.html
```

The API is organized into the following main sections:
- Authentication (/api/auth/*)
- Users (/api/users/*)
- Products (/api/products/*)
- Orders (/api/orders/*)
- Categories (/api/categories/*)
- Reviews (/api/reviews/*)
- Producers (/api/producers/*)
- Analytics (/api/analytics/*)
- Support (/api/support/*)
- Notifications (/api/notifications/*)

## Security
The API uses JWT-based authentication with cookies for secure access. Different endpoints require different role permissions.

## Contributing
Contributions are welcome! Feel free to submit pull requests or open issues to improve the project.

## License
This project is open source and available under the [MIT License](https://opensource.org/licenses/MIT).
