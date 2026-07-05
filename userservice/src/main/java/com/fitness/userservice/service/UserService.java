package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;

public interface UserService {

	UserResponse register(RegisterRequest request);

	UserResponse findByUserId(String userId);

	Boolean existByUserId(String userId);

}
