package com.fitness.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fitness.userservice.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>{

	Boolean existsByEmail(String email);

	Boolean existsByKeycloakId(String userId);

	User findByEmail(String email);

}
