package com.fitness.aiservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Internal Load-Balanced Client Builder
     * Marked with @LoadBalanced. Use this for microservice-to-microservice communication
     * (e.g., calling http://user-service/api/v1/...).
     * @Primary ensures this is chosen by default if no qualifier is specified.
     */
    @Bean
    @LoadBalanced
    @Primary
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * External Standard Client Builder
     * This remains a clean, untouched WebClient builder. Use this for public internet APIs
     * like Google Gemini (https://generativelanguage.googleapis.com).
     */
    @Bean(name = "externalWebClientBuilder")
    public WebClient.Builder externalWebClientBuilder() {
        return WebClient.builder();
    }
}