package com.orderflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Application-wide beans that aren't tied to a single adapter. The use-case
 * services are picked up by component scan ({@code @Service}); only cross-cutting
 * collaborators like the clock are declared here. {@code @EnableScheduling}
 * activates the outbox relay.
 */
@Configuration
@EnableScheduling
public class BeanConfiguration {

    /** A single UTC clock, injected everywhere time is needed, so behaviour is testable. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
