package com.example.microservices.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Declarative HTTP client for product-service.
 * The service name is resolved via Eureka + Spring Cloud LoadBalancer,
 * so calls are client-side load balanced across product-service instances.
 */
@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);

    @PutMapping("/api/products/{id}/decrease-stock")
    ProductDto decreaseStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
