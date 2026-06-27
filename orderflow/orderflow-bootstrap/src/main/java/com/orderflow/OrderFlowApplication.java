package com.orderflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point. Living in the root {@code com.orderflow} package lets
 * component scanning, entity scanning and Spring Data repositories discover the
 * infrastructure adapters without extra configuration.
 */
@SpringBootApplication
public class OrderFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderFlowApplication.class, args);
    }
}
