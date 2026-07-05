package com.fitness.aiservice.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeminiService {

    private final WebClient webClient;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    // We explicitly instruct Spring to pass the 'externalWebClientBuilder' here
    public GeminiService (@Qualifier("externalWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public String getRecommendations(String details) {
        Map<String, Object> requestBody = Map.of("contents", new Object[]{
                                                Map.of("parts", new Object[]{
                                                        Map.of("text", details)
                                                    })
                                            });
                                            
        String response = webClient.post()
                .uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
                
        return response;
    }
}