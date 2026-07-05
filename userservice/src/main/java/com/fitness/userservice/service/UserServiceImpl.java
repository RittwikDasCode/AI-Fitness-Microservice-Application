package com.fitness.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.models.User;
import com.fitness.userservice.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

	@Autowired
	private final UserRepository userRepository;
	
	@Override
	public UserResponse register(RegisterRequest request) {
		
		if(userRepository.existsByEmail(request.getEmail())) {
			User existingUser = userRepository.findByEmail(request.getEmail());
			UserResponse userResponse = new UserResponse();
			userResponse.setId(existingUser.getId());
			userResponse.setEmail(existingUser.getEmail());
			userResponse.setFirstName(existingUser.getFirstName());
			userResponse.setLastName(existingUser.getLastName());
			userResponse.setPassword(existingUser.getPassword());
			userResponse.setCreatedAt(existingUser.getCreatedAt());
			userResponse.setUpdatedAt(existingUser.getUpdatedAt());
			
			return userResponse;
		}
		
		User user = new User();
		user.setEmail(request.getEmail());
		user.setFirstName(request.getFirstName());
		user.setKeycloakId(request.getKeycloakId());
		user.setLastName(request.getLastName());
		user.setPassword(request.getPassword());
		
		User savedUser = userRepository.save(user);
		UserResponse userResponse = new UserResponse();
		userResponse.setId(savedUser.getId());
		userResponse.setEmail(savedUser.getEmail());
		userResponse.setFirstName(savedUser.getFirstName());
		userResponse.setKeycloakId(savedUser.getKeycloakId());
		userResponse.setLastName(savedUser.getLastName());
		userResponse.setPassword(savedUser.getPassword());
		userResponse.setCreatedAt(savedUser.getCreatedAt());
		userResponse.setUpdatedAt(savedUser.getUpdatedAt());
		
		return userResponse;
	}
	@Override
	public UserResponse findByUserId(String userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User Not Found!"));
		UserResponse userResponse = new UserResponse();
		userResponse.setId(user.getId());
		userResponse.setEmail(user.getEmail());
		userResponse.setFirstName(user.getFirstName());
		userResponse.setLastName(user.getLastName());
		userResponse.setPassword(user.getPassword());
		userResponse.setCreatedAt(user.getCreatedAt());
		userResponse.setUpdatedAt(user.getUpdatedAt());
		
		return userResponse;
	}
	@Override
	public Boolean existByUserId(String userId) {
		log.info("Calling User Service for {}", userId);
		return userRepository.existsByKeycloakId(userId);
	}

}
