package com.fitness.activityservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

	@Autowired
	private final WebClient userServiceWebClient;
	
	public boolean validateUser(String userId) {
		log.info("Calling User Service for {}", userId);
		try {
			return userServiceWebClient.get()
					.uri("/api/users/{userId}/validate", userId)
					.retrieve()
					.bodyToMono(Boolean.class)
					.block();
		}catch(WebClientException e){
			e.printStackTrace();
		}
		return false;
	}
}
