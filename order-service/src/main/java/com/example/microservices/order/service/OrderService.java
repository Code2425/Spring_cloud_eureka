package com.example.microservices.order.service;

import com.example.microservices.order.client.ProductClient;
import com.example.microservices.order.client.ProductDto;
import com.example.microservices.order.dto.CreateOrderRequest;
import com.example.microservices.order.dto.OrderDto;
import com.example.microservices.order.entity.Order;
import com.example.microservices.order.exception.InsufficientStockException;
import com.example.microservices.order.exception.ResourceNotFoundException;
import com.example.microservices.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public List<OrderDto> findAll() {
        return orderRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public OrderDto findById(Long id) {
        return toDto(getOrderOrThrow(id));
    }

    /**
     * Creates an order: fetches the product via Feign (Eureka + load balancer),
     * validates stock, decrements stock via product-service, then persists the order.
     */
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        // Service-to-service call #1: read product (price + stock)
        ProductDto product = productClient.getProductById(request.getProductId());

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + request.getProductId()
                            + ": requested " + request.getQuantity()
                            + ", available " + product.getStockQuantity());
        }

        // Service-to-service call #2: decrement stock in product-service
        productClient.decreaseStock(request.getProductId(), request.getQuantity());

        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setStatus("CONFIRMED");
        order.setCreatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return toDto(saved, product.getName());
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    private OrderDto toDto(Order order) {
        return toDto(order, null);
    }

    private OrderDto toDto(Order order, String productName) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setProductId(order.getProductId());
        dto.setProductName(productName);
        dto.setQuantity(order.getQuantity());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}
