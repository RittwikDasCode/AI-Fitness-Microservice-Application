package com.fitness.aiservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ActivityAIService {

	private final GeminiService geminiService;
	
	public Recommendation generateRecommendation(Activity activity) {
		String prompt = createPromptForActivity(activity);
		String aiResponse = geminiService.getRecommendations(prompt);
		log.info("Response from AI: {}", aiResponse);
		return processAIResponse(activity, aiResponse);
	}

	private Recommendation processAIResponse(Activity activity, String aiResponse) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(aiResponse);
			JsonNode textNode = rootNode.path("candidates")
					.get(0)
					.path("content")
					.get("parts")
					.get(0)
					.path("text");
			
			String jsonContent = textNode.asText()
					.replaceAll("```json\\n", "")
					.replaceAll("\\n```", "")
					.trim();
			log.info("Response from the CLEANED AI: {}" + jsonContent);
			
			JsonNode analysisJson = mapper.readTree(jsonContent);
			JsonNode analysisNode = analysisJson.path("analysis");
			StringBuilder fullAnalysis = new StringBuilder();
			addAnalysisSection(fullAnalysis, analysisNode, "overAll", "OverAll:");
			addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace:");
			addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate:");
			addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories Burned:");
			
			List<String> improvements = extractImprovements(analysisJson.path("improvements"));
			List<String> suggenstions = extractSuggenstions(analysisJson.path("suggestions"));
			List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));
			
			return Recommendation.builder()
					.activityId(activity.getId())
					.userId(activity.getUserId())
					.type(activity.getType().toString())
					.recommendation(fullAnalysis.toString().trim())
					.improvements(improvements)
					.suggestions(suggenstions)
					.safety(safety)
					.createdAt(LocalDateTime.now())
					.build();
			
		}catch(Exception e) {
			e.printStackTrace();
			return createDefaultRecommendation(activity);
		}
		
	}

	private Recommendation createDefaultRecommendation(Activity activity) {
		return Recommendation.builder()
				.activityId(activity.getId())
				.userId(activity.getUserId())
				.type(activity.getType().toString())
				.recommendation("Unable to generate detailed analysis")
				.improvements(Collections.singletonList("Continue with you current routine!"))
				.suggestions(Collections.singletonList("Consider consulting an Fitness Consultant!"))
				.safety(Collections.singletonList("Always warm up before the exercises and stay hydrated!"))
				.createdAt(LocalDateTime.now())
				.build();
	}

	private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
		List<String> safety = new ArrayList<String>();
		if(safetyNode.isArray()){
			safetyNode.forEach(item -> safety.add(item.asText()));
		}
		return safety.isEmpty() ? Collections.singletonList("Follow General Safety Guidelines!") : safety;
	}

	private List<String> extractSuggenstions(JsonNode suggenstionsNode) {
		List<String> suggenstions = new ArrayList<String>();
		if(suggenstionsNode.isArray()){
			suggenstionsNode.forEach(suggenstion -> {
			String workout = suggenstion.path("workout").asText();
			String description = suggenstion.path("description").asText();
			suggenstions.add(String.format("%s: %s", workout, description));
			});
		}
		return suggenstions.isEmpty() ? Collections.singletonList("No Specific Suggenstions Provided!") : suggenstions;
	}

	private List<String> extractImprovements(JsonNode improvementsNode) {
		List<String> improvements = new ArrayList<String>();
		if(improvementsNode.isArray()){
			improvementsNode.forEach(improvement -> {
			String area = improvement.path("area").asText();
			String detail = improvement.path("recommendation").asText();
			improvements.add(String.format("%s: %s", area, detail));
			});
		}
		return improvements.isEmpty() ? Collections.singletonList("No Specific Improvements Provided!") : improvements;
	}

	// "overall": "This was an excellent"
	// Overall: This was an excellent
	private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
		if(!analysisNode.path(key).isMissingNode()) {
			fullAnalysis.append(prefix)
			.append(analysisNode.path(key).asText())
			.append("\n\n");
		}
		
	}

	private String createPromptForActivity(Activity activity) {
		
        return String.format("""
        Analyze this fitness activity and provide detailed recommendations in the following EXACT JSON format:
        
        Do NOT wrap the response in markdown code blocks like ```json ... ```. 
        Return ONLY the raw JSON string starting with { and ending with }.
        
        EXPECTED JSON STRUCTURE:
        {
          "analysis": {
            "overall": "Overall analysis here",
            "pace": "Pace analysis here",
            "heartRate": "Heart rate analysis here",
            "caloriesBurned": "Calories analysis here"
          },
          "improvements": [
            {
              "area": "Area name",
              "recommendation": "Detailed recommendation"
            }
          ],
          "suggestions": [
            {
              "workout": "Workout name",
              "description": "Detailed workout description"
            }
          ],
          "safety": [
            "Safety point 1",
            "Safety point 2"
          ]
        }

        Analyze this activity:
        Activity Type: %s
        Duration: %d minutes
        Calories Burned: %d
        Additional Metrics: %s
        
        Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
        Ensure the response follows the EXACT JSON format shown above.
        """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
	}
}
