package com.example.microservices.order.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public feign.codec.ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Propagates the incoming X-Correlation-ID header to downstream
     * product-service calls so a request can be traced across services.
     */
    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    String correlationId = attributes.getRequest().getHeader("X-Correlation-ID");
                    if (correlationId != null && !correlationId.isBlank()) {
                        template.header("X-Correlation-ID", correlationId);
                    }
                }
            }
        };
    }
}
