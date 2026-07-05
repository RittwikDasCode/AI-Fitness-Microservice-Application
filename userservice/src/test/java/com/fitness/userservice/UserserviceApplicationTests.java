package com.fitness.userservice;

import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.models.User;
import com.fitness.userservice.repository.UserRepository;
import com.fitness.userservice.service.UserServiceImpl;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
class UserserviceApplicationTests {

	@Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private User mockUser;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@fitness.com");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPassword("securePassword");
        registerRequest.setKeycloakId("kc-123");

        mockUser = new User();
        mockUser.setId("user-123");
        mockUser.setEmail("test@fitness.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setPassword("securePassword");
        mockUser.setKeycloakId("kc-123");
        mockUser.setCreatedAt(now);
        mockUser.setUpdatedAt(now);
    }

    // ==========================================
    // register(RegisterRequest request) Tests
    // ==========================================

    @Test
    void shouldReturnExistingUserWhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(mockUser);

        // Act
        UserResponse response = userService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(mockUser.getId(), response.getId());
        assertEquals(mockUser.getEmail(), response.getEmail());
        assertEquals(mockUser.getFirstName(), response.getFirstName());
        assertEquals(mockUser.getLastName(), response.getLastName());
        assertEquals(mockUser.getPassword(), response.getPassword());
        assertEquals(mockUser.getCreatedAt(), response.getCreatedAt());
        assertEquals(mockUser.getUpdatedAt(), response.getUpdatedAt());

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
        verify(userRepository, times(0)).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldSaveAndReturnNewUserWhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        UserResponse response = userService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(mockUser.getId(), response.getId());
        assertEquals(mockUser.getEmail(), response.getEmail());
        assertEquals(mockUser.getFirstName(), response.getFirstName());
        assertEquals(mockUser.getLastName(), response.getLastName());
        assertEquals(mockUser.getKeycloakId(), response.getKeycloakId());
        assertEquals(mockUser.getPassword(), response.getPassword());
        assertEquals(mockUser.getCreatedAt(), response.getCreatedAt());
        assertEquals(mockUser.getUpdatedAt(), response.getUpdatedAt());

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(0)).findByEmail(any(String.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRegisterRequestIsNull() {
        // Arrange, Act & Assert
        assertThrows(NullPointerException.class, () -> {
            userService.register(null);
        });
        
        verifyNoMoreInteractions(userRepository);
    }

    // ==========================================
    // findByUserId(String userId) Tests
    // ==========================================

    @Test
    void shouldReturnUserResponseWhenUserIdExists() {
        // Arrange
        String userId = "user-123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        UserResponse response = userService.findByUserId(userId);

        // Assert
        assertNotNull(response);
        assertEquals(mockUser.getId(), response.getId());
        assertEquals(mockUser.getEmail(), response.getEmail());
        assertEquals(mockUser.getFirstName(), response.getFirstName());
        assertEquals(mockUser.getLastName(), response.getLastName());
        assertEquals(mockUser.getPassword(), response.getPassword());
        assertEquals(mockUser.getCreatedAt(), response.getCreatedAt());
        assertEquals(mockUser.getUpdatedAt(), response.getUpdatedAt());

        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUserIdDoesNotExist() {
        // Arrange
        String userId = "non-existent-id";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findByUserId(userId);
        });

        assertEquals("User Not Found!", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    // ==========================================
    // existByUserId(String userId) Tests
    // ==========================================

    @Test
    void shouldReturnTrueWhenKeycloakIdExists() {
        // Arrange
        String keycloakId = "kc-123";
        when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(true);

        // Act
        Boolean result = userService.existByUserId(keycloakId);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByKeycloakId(keycloakId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldReturnFalseWhenKeycloakIdDoesNotExist() {
        // Arrange
        String keycloakId = "kc-absent";
        when(userRepository.existsByKeycloakId(keycloakId)).thenReturn(false);

        // Act
        Boolean result = userService.existByUserId(keycloakId);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsByKeycloakId(keycloakId);
        verifyNoMoreInteractions(userRepository);
    }


}
