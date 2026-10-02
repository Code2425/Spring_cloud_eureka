package com.example.microservices.order.client;

import java.math.BigDecimal;

/**
 * Minimal view of the product-service ProductDto, used by the Feign client.
 */
public class ProductDto {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
