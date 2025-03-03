// src/main/java/com/ecommerce/paymentservice/model/Payment.java
package com.ecommerce.paymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String paymentMethod; // CREDIT_CARD, PAYPAL, etc.
    
    @Column(nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING
    
    private String transactionId;
}

// src/main/java/com/ecommerce/paymentservice/repository/PaymentRepository.java
package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, String status);
}

// src/main/java/com/ecommerce/paymentservice/dto/PaymentDTO.java
package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String status;
    private String transactionId;
}

// src/main/java/com/ecommerce/paymentservice/dto/PaymentRequest.java
package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String cardNumber;
    private String cardExpiryDate;
    private String cvv;
}

// src/main/java/com/ecommerce/paymentservice/dto/OrderDTO.java
package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
}

// src/main/java/com/ecommerce/paymentservice/client/OrderServiceClient.java
package com.ecommerce.paymentservice.client;

import com.ecommerce.paymentservice.dto.OrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String ORDER_SERVICE_URL = "http://localhost:8083/api/orders";
    
    public OrderDTO getOrderById(Long orderId) {
        return restTemplate.getForObject(ORDER_SERVICE_URL + "/" + orderId, OrderDTO.class);
    }
    
    public void updateOrderStatus(Long orderId, String status) {
        restTemplate.put(ORDER_SERVICE_URL + "/" + orderId + "/status?status=" + status, null);
    }
}

// src/main/java/com/ecommerce/paymentservice/service/PaymentService.java
package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.dto.OrderDTO;
import com.ecommerce.paymentservice.dto.PaymentDTO;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private OrderServiceClient orderServiceClient;
    
    @Transactional
    public PaymentDTO processPayment(PaymentRequest paymentRequest) {
        // Validate order
        OrderDTO order = orderServiceClient.getOrderById(paymentRequest.getOrderId());
        
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        
        if (!order.getUserId().equals(paymentRequest.getUserId())) {
            throw new RuntimeException("Order does not belong to this user");
        }
        
        // Check if payment already exists and is successful
        paymentRepository.findByOrderIdAndStatus(paymentRequest.getOrderId(), "SUCCESS")
                .ifPresent(p -> {
                    throw new RuntimeException("Payment already processed for this order");
                });
        
        // Process payment (in a real scenario, you'd integrate with a payment gateway)
        boolean paymentSuccess = processPaymentWithGateway(paymentRequest);
        
        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserId(paymentRequest.getUserId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(paymentSuccess ? "SUCCESS" : "FAILED");
        
        if (paymentSuccess) {
            // Generate transaction ID
            payment.setTransactionId(UUID.randomUUID().toString());
            
            // Update order status
            orderServiceClient.updateOrderStatus(paymentRequest.getOrderId(), "PAID");
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        
        return convertToDTO(savedPayment);
    }
    
    private boolean processPaymentWithGateway(PaymentRequest paymentRequest) {
        // In a real scenario, this would integrate with a payment gateway
        // For this example, we'll simulate a successful payment
        return true;
    }
    
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        return convertToDTO(payment);
    }
    
    public List<PaymentDTO> getPaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        
        return payments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<PaymentDTO> getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        
        return payments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(payment.getId());
        paymentDTO.setOrderId(payment.getOrderId());
        paymentDTO.setUserId(payment.getUserId());
        paymentDTO.setAmount(payment.getAmount());
        paymentDTO.setPaymentMethod(payment.getPaymentMethod());
        paymentDTO.setPaymentDate(payment.getPaymentDate());
        paymentDTO.setStatus(payment.getStatus());
        paymentDTO.setTransactionId(payment.getTransactionId());
        
        return paymentDTO;
    }
}

// src/main/java/com/ecommerce/paymentservice/controller/PaymentController.java
package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentDTO;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(@RequestBody PaymentRequest paymentRequest) {
        PaymentDTO payment = paymentService.processPayment(paymentRequest);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        PaymentDTO payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByUserId(@PathVariable Long userId) {
        List<PaymentDTO> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<PaymentDTO> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }
}

// src/main/java/com/ecommerce/paymentservice/config/RestTemplateConfig.java
package com.ecommerce.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// src/main/java/com/ecommerce/paymentservice/config/SwaggerConfig.java
package com.ecommerce.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .version("1.0")
                        .description("API for Payment Service in E-commerce application"));
    }
}

// src/main/java/com/ecommerce/paymentservice/PaymentServiceApplication.java
package com.ecommerce.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

// src/main/resources/application.properties
spring.application.name=payment-service
server.port=8084

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/payment_service_db
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
