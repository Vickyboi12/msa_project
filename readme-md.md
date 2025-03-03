# E-commerce Microservices Application

This project implements a simple e-commerce application using microservices architecture with Spring Boot. The application consists of four core microservices that handle different aspects of an e-commerce platform.

## Microservices Architecture

### 1. User Service
- **Port**: 8081
- **Database**: MySQL (user_service_db)
- **Purpose**: Manages user accounts, authentication, and user data.
- **API Endpoints**:
  - `POST /api/users/register` - Register a new user
  - `GET /api/users/{id}` - Get user by ID
  - `GET /api/users/username/{username}` - Get user by username

### 2. Product Service
- **Port**: 8082
- **Database**: MySQL (product_service_db)
- **Purpose**: Manages product catalog, inventory, and product details.
- **API Endpoints**:
  - `POST /api/products` - Create a new product
  - `GET /api/products/{id}` - Get product by ID
  - `GET /api/products` - Get all products
  - `GET /api/products/category/{category}` - Get products by category
  - `GET /api/products/search?keyword={keyword}` - Search products
  - `PUT /api/products/{id}` - Update product
  - `DELETE /api/products/{id}` - Delete product
  - `PUT /api/products/{id}/stock?quantity={quantity}` - Update product stock

### 3. Order Service
- **Port**: 8083
- **Database**: MySQL (order_service_db)
- **Purpose**: Handles order creation, tracking, and management.
- **API Endpoints**:
  - `POST /api/orders` - Create a new order
  - `GET /api/orders/{id}` - Get order by ID
  - `GET /api/orders/user/{userId}` - Get orders by user ID
  - `PUT /api/orders/{id}/status?status={status}` - Update order status

### 4. Payment Service
- **Port**: 8084
- **Database**: MySQL (payment_service_db)
- **Purpose**: Processes payments and manages payment records.
- **API Endpoints**:
  - `POST /api/payments/process` - Process a payment
  - `GET /api/payments/{id}` - Get payment by ID
  - `GET /api/payments/user/{userId}` - Get payments by user ID
  - `GET /api/payments/order/{orderId}` - Get payments for an order

## Technology Stack

- **Framework**: Spring Boot
- **Database**: MySQL
- **Database ORM**: Spring Data JPA with Hibernate
- **Communication**: RestTemplate for inter-service communication
- **API Documentation**: Swagger/OpenAPI

## Setup and Running

### Prerequisites
- JDK 17 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher

### Database Setup
Create four MySQL databases:
```sql
CREATE DATABASE user_service_db;
CREATE DATABASE product_service_db;
CREATE DATABASE order_service_db;
CREATE DATABASE payment_service_db;
```

### Configuration
You may need to update the database connection details in each microservice's `application.properties` file:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/{service_db_name}
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Building and Running the Services
1. Navigate to each microservice directory
2. Build the service: `mvn clean package`
3. Run the service: `java -jar target/{service-name}-0.0.1-SNAPSHOT.jar`

Start the services in the following order:
1. User Service
2. Product Service
3. Order Service
4. Payment Service

### API Documentation
After starting each service, Swagger UI is available at:
- http://localhost:8081/swagger-ui.html (User Service)
- http://localhost:8082/swagger-ui.html (Product Service)
- http://localhost:8083/swagger-ui.html (Order Service)
- http://localhost:8084/swagger-ui.html (Payment Service)

## Service Communication Flow

1. A user authenticates via the User Service
2. The user browses products via the Product Service
3. The user creates an order via the Order Service
   - Order Service communicates with Product Service to check and update stock
4. The user makes a payment via the Payment Service
   - Payment Service communicates with Order Service to update order status

## Future Enhancements

- Add Eureka Service Discovery
- Implement API Gateway for unified entry point
- Add Circuit Breaker pattern for fault tolerance
- Implement distributed logging
- Add container support with Docker and Kubernetes
