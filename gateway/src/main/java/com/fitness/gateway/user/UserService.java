package com.fitness.gateway.user;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	@Autowired
	private final WebClient userServiceWebClient;
	
	public Mono<Boolean> validateUser(String userId) {
		log.info("Calling User Service for {}", userId);

			return userServiceWebClient.get()
					.uri("/api/users/{userId}/validate", userId)
					.retrieve()
					.bodyToMono(Boolean.class)
					.onErrorResume(WebClientResponseException.class, e -> {
						if(e.getStatusCode() == HttpStatus.NOT_FOUND)
							return Mono.error(new RuntimeException("User Not Found! " + userId));
						
						else if(e.getStatusCode() == HttpStatus.BAD_REQUEST)
							return Mono.error(new RuntimeException("Invalid User! " + userId));
						
						return Mono.error(new RuntimeException("Unexpected Error! " + userId));
					});
		
		
	}

	public Mono<UserResponse> registerUser(RegisterRequest registerRequest) {
		
		log.info("Calling User Service for {}", registerRequest.getEmail());

		return userServiceWebClient.post()
				.uri("/api/users/register")
				.bodyValue(registerRequest)
				.retrieve()
				.bodyToMono(UserResponse.class)
				.onErrorResume(WebClientResponseException.class, e -> {
					if(e.getStatusCode() == HttpStatus.BAD_REQUEST)
						return Mono.error(new RuntimeException("BAD REQUEST! " + e.getMessage()));
					
					return Mono.error(new RuntimeException("Unexpected Error! " + e.getMessage()));
				});
	}
}
