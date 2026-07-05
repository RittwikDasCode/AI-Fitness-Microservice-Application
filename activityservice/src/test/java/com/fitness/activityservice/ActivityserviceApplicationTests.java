package com.fitness.activityservice;

import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.repository.ActivityRepository;
import com.fitness.activityservice.service.ActivityServiceImpl;
import com.fitness.activityservice.service.UserValidationService;

@ExtendWith(MockitoExtension.class)
class ActivityserviceApplicationTests {

	@Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private KafkaTemplate<String, Activity> kafkaTemplate;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private ActivityRequest activityRequest;
    private Activity mockActivity;
    private final String testTopic = "fitness-activity-topic";

    @BeforeEach
    void setUp() {
        // Explicitly inject the @Value property value into our service instance
        ReflectionTestUtils.setField(activityService, "topicName", testTopic);

        LocalDateTime now = LocalDateTime.now();

        activityRequest = new ActivityRequest();
        activityRequest.setUserId("user-999");
        activityRequest.setType(ActivityType.RUNNING);
        activityRequest.setDuration(45);
        activityRequest.setCaloriesBurned(400);
        activityRequest.setStartTime(now);
        //activityRequest.setAdditionalMetrics("HeartRate: 145");

        mockActivity = Activity.builder()
                .id("act-111")
                .userId("user-999")
                .type(ActivityType.RUNNING)
                .duration(45)
                .caloriesBurned(400)
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                //.additionalMetrics("HeartRate: 145")
                .build();
    }

    // ==========================================
    // trackActivity(ActivityRequest request) Tests
    // ==========================================

    @Test
    void shouldTrackActivitySuccessfullyWhenUserIsValid() {
        // Arrange
        when(userValidationService.validateUser(activityRequest.getUserId())).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);

        // Act
        ActivityResponse response = activityService.trackActivity(activityRequest);

        // Assert
        assertNotNull(response);
        assertEquals(mockActivity.getId(), response.getId());
        assertEquals(mockActivity.getUserId(), response.getUserId());
        assertEquals(mockActivity.getType(), response.getType());
        assertEquals(mockActivity.getDuration(), response.getDuration());
        assertEquals(mockActivity.getCaloriesBurned(), response.getCaloriesBurned());
        assertEquals(mockActivity.getStartTime(), response.getStartTime());
        assertEquals(mockActivity.getAdditionalMetrics(), response.getAdditionalMetrics());

        verify(userValidationService, times(1)).validateUser(activityRequest.getUserId());
        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(kafkaTemplate, times(1)).send(eq(testTopic), eq(mockActivity.getUserId()), eq(mockActivity));
        verifyNoMoreInteractions(userValidationService, activityRepository, kafkaTemplate);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUserIsInvalid() {
        // Arrange
        when(userValidationService.validateUser(activityRequest.getUserId())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            activityService.trackActivity(activityRequest);
        });

        assertEquals("Invalid User: " + activityRequest.getUserId(), exception.getMessage());
        verify(userValidationService, times(1)).validateUser(activityRequest.getUserId());
        verifyNoInteractions(activityRepository, kafkaTemplate);
    }

    @Test
    void shouldTrackActivitySuccessfullyEvenWhenKafkaFails() {
        // Arrange
        when(userValidationService.validateUser(activityRequest.getUserId())).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(mockActivity);
        
        // Force Kafka to fail by throwing an exception inside the try-block
        when(kafkaTemplate.send(eq(testTopic), eq(mockActivity.getUserId()), eq(mockActivity)))
                .thenThrow(new RuntimeException("Kafka Broker Unavailable"));

        // Act
        ActivityResponse response = activityService.trackActivity(activityRequest);

        // Assert (Method should gracefully catch the exception and still return data)
        assertNotNull(response);
        assertEquals(mockActivity.getId(), response.getId());
        
        verify(userValidationService, times(1)).validateUser(activityRequest.getUserId());
        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(kafkaTemplate, times(1)).send(eq(testTopic), eq(mockActivity.getUserId()), eq(mockActivity));
    }

    // ==========================================
    // getUserActivities(String userId) Tests
    // ==========================================

    @Test
    void shouldReturnListofActivityResponsesWhenUserHasActivities() {
        // Arrange
        String userId = "user-999";
        when(activityRepository.findByUserId(userId)).thenReturn(List.of(mockActivity));

        // Act
        List<ActivityResponse> responses = activityService.getUserActivities(userId);

        // Assert
        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(mockActivity.getId(), responses.get(0).getId());

        verify(activityRepository, times(1)).findByUserId(userId);
        verifyNoMoreInteractions(activityRepository);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {
        // Arrange
        String userId = "user-empty";
        when(activityRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<ActivityResponse> responses = activityService.getUserActivities(userId);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(activityRepository, times(1)).findByUserId(userId);
        verifyNoMoreInteractions(activityRepository);
    }

    // ==========================================
    // getActivityById(String activityId) Tests
    // ==========================================

    @Test
    void shouldReturnActivityResponseWhenActivityIdExists() {
        // Arrange
        String activityId = "act-111";
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(mockActivity));

        // Act
        ActivityResponse response = activityService.getActivityById(activityId);

        // Assert
        assertNotNull(response);
        assertEquals(mockActivity.getId(), response.getId());
        assertEquals(mockActivity.getType(), response.getType());

        verify(activityRepository, times(1)).findById(activityId);
        verifyNoMoreInteractions(activityRepository);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenActivityIdDoesNotExist() {
        // Arrange
        String activityId = "absent-act";
        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            activityService.getActivityById(activityId);
        });

        assertEquals("Activity not found with id: " + activityId, exception.getMessage());
        verify(activityRepository, times(1)).findById(activityId);
        verifyNoMoreInteractions(activityRepository);
    }

}
