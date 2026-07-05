package com.fitness.activityservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService{

	@Autowired
	private final ActivityRepository activityRepository;
	
	@Autowired
	private final UserValidationService userValidationService;	
	
	private final KafkaTemplate<String, Activity> kafkaTemplate;
	
	@Value("${kafka.topic.name}")
	private String topicName;
	
	@Override
	public ActivityResponse trackActivity(ActivityRequest request) {
		
		boolean isValidUser = userValidationService.validateUser(request.getUserId());
		
		if(!isValidUser) {
			throw new RuntimeException("Invalid User: " + request.getUserId());
		}
		
		Activity activity = Activity.builder()
				.userId(request.getUserId())
				.type(request.getType())
				.duration(request.getDuration())
				.caloriesBurned(request.getCaloriesBurned())
				.startTime(request.getStartTime())
				.additionalMetrics(request.getAdditionalMetrics())
				.build();
		Activity savedActivity = activityRepository.save(activity);
		
		try {
			kafkaTemplate.send(topicName, savedActivity.getUserId(), savedActivity);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return mapToResponse(savedActivity);
		
	}
	
	private ActivityResponse mapToResponse(Activity activity) {
		ActivityResponse response = new ActivityResponse();
		response.setId(activity.getId());
		response.setUserId(activity.getUserId());
		response.setType(activity.getType());
		response.setDuration(activity.getDuration());
		response.setCaloriesBurned(activity.getCaloriesBurned());
		response.setStartTime(activity.getStartTime());
		response.setCreatedAt(activity.getCreatedAt());
		response.setUpdatedAt(activity.getUpdatedAt());
		response.setAdditionalMetrics(activity.getAdditionalMetrics());
		
		return response;
	}

	@Override
	public List<ActivityResponse> getUserActivities(String userId) {
        List<Activity> activities = activityRepository.findByUserId(userId);
        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
	}

	@Override
	public ActivityResponse getActivityById(String activityId) {
        return activityRepository.findById(activityId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));
    	
	}
	
}
