// src/main/java/com/ecommerce/orderservice/model/Order.java
package com.ecommerce.orderservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private LocalDateTime orderDate;
    
    @Column(nullable = false)
    private String status; // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    private String shippingAddress;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
}

// src/main/java/com/ecommerce/orderservice/model/OrderItem.java
package com.ecommerce.orderservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal price;
}

// src/main/java/com/ecommerce/orderservice/repository/OrderRepository.java
package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}

// src/main/java/com/ecommerce/orderservice/dto/OrderItemDTO.java
package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}

// src/main/java/com/ecommerce/orderservice/dto/OrderDTO.java
package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDTO> orderItems;
}

// src/main/java/com/ecommerce/orderservice/dto/CreateOrderRequest.java
package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long userId;
    private String shippingAddress;
    private List<OrderItemRequest> items;
}

// src/main/java/com/ecommerce/orderservice/dto/OrderItemRequest.java
package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
}

// src/main/java/com/ecommerce/orderservice/dto/ProductDTO.java
package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
}

// src/main/java/com/ecommerce/orderservice/client/ProductServiceClient.java
package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products";
    
    public ProductDTO getProductById(Long productId) {
        return restTemplate.getForObject(PRODUCT_SERVICE_URL + "/" + productId, ProductDTO.class);
    }
    
    public boolean updateStock(Long productId, Integer quantity) {
        try {
            restTemplate.put(PRODUCT_SERVICE_URL + "/" + productId + "/stock?quantity=" + quantity, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

// src/main/java/com/ecommerce/orderservice/service/OrderService.java
package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest createOrderRequest) {
        Order order = new Order();
        order.setUserId(createOrderRequest.getUserId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setShippingAddress(createOrderRequest.getShippingAddress());
        
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : createOrderRequest.getItems()) {
            ProductDTO product = productServiceClient.getProductById(itemRequest.getProductId());
            
            // Check if there's enough stock
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }
            
            // Update stock in product service
            boolean stockUpdated = productServiceClient.updateStock(
                    itemRequest.getProductId(), 
                    itemRequest.getQuantity()
            );
            
            if (!stockUpdated) {
                throw new RuntimeException("Failed to update stock for product: " + product.getName());
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            
            orderItems.add(orderItem);
        }
        
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);
        
        Order savedOrder = orderRepository.save(order);
        
        return convertToDTO(savedOrder);
    }
    
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        return convertToDTO(order);
    }
    
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public OrderDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        return convertToDTO(updatedOrder);
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setUserId(order.getUserId());
        orderDTO.setOrderDate(order.getOrderDate());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setShippingAddress(order.getShippingAddress());
        
        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        orderDTO.setOrderItems(orderItemDTOs);
        
        return orderDTO;
    }
    
    private OrderItemDTO convertToDTO(OrderItem orderItem) {
        OrderItemDTO orderItemDTO = new OrderItemDTO();
        orderItemDTO.setId(orderItem.getId());
        orderItemDTO.setProductId(orderItem.getProductId());
        orderItemDTO.setProductName(orderItem.getProductName());
        orderItemDTO.setQuantity(orderItem.getQuantity());
        orderItemDTO.setPrice(orderItem.getPrice());
        return orderItemDTO;
    }
}

// src/main/java/com/ecommerce/orderservice/controller/OrderController.java
package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderDTO;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        OrderDTO createdOrder = orderService.createOrder(createOrderRequest);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO orderDTO = orderService.getOrderById(id);
        return ResponseEntity.ok(orderDTO);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List