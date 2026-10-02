package com.example.microservices.order.client;

import com.example.microservices.order.exception.InsufficientStockException;
import com.example.microservices.order.exception.ResourceNotFoundException;
import com.example.microservices.order.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Translates product-service HTTP error statuses into domain exceptions
 * handled by the GlobalExceptionHandler.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 404:
                return new ResourceNotFoundException("Product not found in product-service");
            case 409:
                return new InsufficientStockException("Insufficient stock reported by product-service");
            default:
                if (response.status() >= 500) {
                    return new ServiceUnavailableException(
                            "product-service returned status " + response.status());
                }
                return defaultDecoder.decode(methodKey, response);
        }
    }
}
