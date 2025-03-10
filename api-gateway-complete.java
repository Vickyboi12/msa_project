package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Complete API Gateway Implementation
 * 
 * ==================== DEPENDENCIES ====================
 * Maven dependencies required:
 * <dependencies>
 *     <dependency>
 *         <groupId>org.springframework.cloud</groupId>
 *         <artifactId>spring-cloud-starter-gateway</artifactId>
 *     </dependency>
 *     <dependency>
 *         <groupId>org.springframework.cloud</groupId>
 *         <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
 *     </dependency>
 * </dependencies>
 * 
 * ==================== APPLICATION PROPERTIES ====================
 * Configuration to be placed in application.properties:
 * server.port=8080
 * spring.application.name=api-gateway
 * eureka.client.service-url.defaultZone=http://localhost:8761/eureka
 * 
 * spring.cloud.gateway.routes[0].id=user-service
 * spring.cloud.gateway.routes[0].uri=lb://user-service
 * spring.cloud.gateway.routes[0].predicates[0]=Path=/api/users/**
 * 
 * spring.cloud.gateway.routes[1].id=product-service
 * spring.cloud.gateway.routes[1].uri=lb://product-service
 * spring.cloud.gateway.routes[1].predicates[0]=Path=/api/products/**
 * 
 * spring.cloud.gateway.routes[2].id=order-service
 * spring.cloud.gateway.routes[2].uri=lb://order-service
 * spring.cloud.gateway.routes[2].predicates[0]=Path=/api/orders/**
 * 
 * spring.cloud.gateway.routes[3].id=payment-service
 * spring.cloud.gateway.routes[3].uri=lb://payment-service
 * spring.cloud.gateway.routes[3].predicates[0]=Path=/api/payments/**
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
