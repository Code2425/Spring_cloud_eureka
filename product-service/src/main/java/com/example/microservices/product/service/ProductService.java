package com.example.microservices.product.service;

import com.example.microservices.product.dto.CreateProductRequest;
import com.example.microservices.product.dto.ProductDto;
import com.example.microservices.product.entity.Product;
import com.example.microservices.product.exception.InsufficientStockException;
import com.example.microservices.product.exception.ResourceNotFoundException;
import com.example.microservices.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> findAll() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto findById(Long id) {
        return toDto(getProductOrThrow(id));
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, CreateProductRequest request) {
        Product product = getProductOrThrow(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        return toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProductOrThrow(id);
        productRepository.delete(product);
    }

    @Transactional
    public ProductDto decreaseStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Product product = getProductOrThrow(id);
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + id + ": requested " + quantity
                            + ", available " + product.getStockQuantity());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        return toDto(productRepository.save(product));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        return dto;
    }
}
