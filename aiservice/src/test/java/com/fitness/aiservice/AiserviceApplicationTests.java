package com.fitness.aiservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.ActivityType;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import com.fitness.aiservice.service.ActivityAIService;
import com.fitness.aiservice.service.ActivityMessageListener;
import com.fitness.aiservice.service.GeminiService;
import com.fitness.aiservice.service.RecommendationServiceImpl;

@ExtendWith(MockitoExtension.class)
class AiserviceApplicationTests {

	// Global Mock Dependencies shared across components
    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private ActivityAIService mockActivityAIService;

    @Mock
    private WebClient.Builder webClientBuilder;

    // Injecting dependencies into respective targets
    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @InjectMocks
    private ActivityAIService activityAIService;

    @InjectMocks
    private ActivityMessageListener activityMessageListener;

    private Recommendation mockRecommendation;
    private Activity mockActivity;
    private String sampleValidAiResponse;

    @BeforeEach
    void globalSetUp() {
        mockRecommendation = new Recommendation();
        mockRecommendation.setId("rec-101");
        mockRecommendation.setUserId("user-777");
        mockRecommendation.setActivityId("act-555");
        mockRecommendation.setRecommendation("Increase pace slightly.");

        mockActivity = new Activity();
        mockActivity.setId("act-abc");
        mockActivity.setUserId("user-xyz");
        mockActivity.setType(ActivityType.RUNNING);
        mockActivity.setDuration(30);
        mockActivity.setCaloriesBurned(350);
        //mockActivity.setAdditionalMetrics("Avg HR: 150");

        sampleValidAiResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "```json\\n{\\n  \\"analysis\\": {\\n    \\"overAll\\": \\"Great work!\\",\\n    \\"pace\\": \\"Steady pacing.\\",\\n    \\"heartRate\\": \\"Perfect cardio range.\\",\\n    \\"caloriesBurned\\": \\"High fat burn.\\"\\n  },\\n  \\"improvements\\": [\\n    {\\"area\\": \\"Stamina\\", \\"recommendation\\": \\"Extend run by 5 mins.\\"}\\n  ],\\n  \\"suggestions\\": [\\n    {\\"workout\\": \\"HIIT\\", \\"description\\": \\"Try interval sprints next.\\"}\\n  ],\\n  \\"safety\\": [\\n    \\"Hydrate during run.\\"\\n  ]\\n}\\n```"
                  }
                ]
              }
            }
          ]
        }
        """;
    }

    // =========================================================================
    // 1. RECOMMENDATION SERVICE IMPL TESTS
    // =========================================================================
    @Nested
    class RecommendationServiceTests {

        @Test
        void shouldReturnListofRecommendationsWhenUserIdExists() {
            String userId = "user-777";
            when(recommendationRepository.findByUserId(userId)).thenReturn(List.of(mockRecommendation));

            List<Recommendation> result = recommendationService.getUserRecommendation(userId);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(recommendationRepository, times(1)).findByUserId(userId);
        }

        @Test
        void shouldReturnEmptyListWhenNoRecommendationsFoundForUser() {
            String userId = "user-no-rec";
            when(recommendationRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            List<Recommendation> result = recommendationService.getUserRecommendation(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnRecommendationWhenActivityIdExists() {
            String activityId = "act-555";
            when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.of(mockRecommendation));

            Recommendation result = recommendationService.getActivityRecommendation(activityId);

            assertNotNull(result);
            assertEquals("rec-101", result.getId());
        }

        
        @Test
        void shouldThrowExceptionWhenRecommendationNotFoundForActivity() {
            String activityId = "missing-act";
            when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                recommendationService.getActivityRecommendation(activityId);
            });

            assertTrue(exception.getMessage().contains("No recommendation found with the activity:"));
        }
    }
    

    // =========================================================================
    // 2. ACTIVITY AI SERVICE (JSON PARSING & GEMINI MAPPER) TESTS
    // =========================================================================
    @Nested
    class ActivityAIServiceTests {

        @Test
        void shouldGenerateFullRecommendationWhenAiReturnsValidStructuredJson() {
            when(geminiService.getRecommendations(anyString())).thenReturn(sampleValidAiResponse);

            Recommendation recommendation = activityAIService.generateRecommendation(mockActivity);

            assertNotNull(recommendation);
            assertEquals("act-abc", recommendation.getActivityId());
            assertTrue(recommendation.getRecommendation().contains("OverAll:Great work!"));
            assertEquals("Stamina: Extend run by 5 mins.", recommendation.getImprovements().get(0));
        }

        @Test
        void shouldReturnPopulatedDefaultCollectionsWhenJsonArraysAreEmpty() {
            String emptyArraysAiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "{\\"analysis\\": {}, \\"improvements\\": [], \\"suggestions\\": [], \\"safety\\": []}" }
                    ]
                  }
                ]
              }
            }
            """;
            when(geminiService.getRecommendations(anyString())).thenReturn(emptyArraysAiResponse);

            Recommendation recommendation = activityAIService.generateRecommendation(mockActivity);

            assertNotNull(recommendation);
            assertEquals("Continue with you current routine!", recommendation.getImprovements().get(0));
            assertEquals("Consider consulting an Fitness Consultant!", recommendation.getSuggestions().get(0));
            assertEquals("Always warm up before the exercises and stay hydrated!", recommendation.getSafety().get(0));
        }

        @Test
        void shouldFallbackToDefaultRecommendationWhenJsonParsingCrashes() {
            when(geminiService.getRecommendations(anyString())).thenReturn("{ malformed JSON string...");

            Recommendation recommendation = activityAIService.generateRecommendation(mockActivity);

            assertNotNull(recommendation);
            assertEquals("Unable to generate detailed analysis", recommendation.getRecommendation());
            assertEquals("Continue with you current routine!", recommendation.getImprovements().get(0));
        }
    }
    

    // =========================================================================
    // 3. KAFKA MESSAGE LISTENER TESTS
    // =========================================================================
    @Nested
    class ActivityMessageListenerTests {

        @Test
        void shouldProcessActivityAndSaveRecommendationWhenEventIsReceived() {
            Activity activity = new Activity();
            activity.setUserId("user-123");

            Recommendation recommendation = Recommendation.builder()
                    .id("rec-999")
                    .userId("user-123")
                    .build();

            when(mockActivityAIService.generateRecommendation(activity)).thenReturn(recommendation);
            when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

            activityMessageListener.processActivity(activity);

            verify(mockActivityAIService, times(1)).generateRecommendation(activity);
            verify(recommendationRepository, times(1)).save(recommendation);
        }
    }



}
