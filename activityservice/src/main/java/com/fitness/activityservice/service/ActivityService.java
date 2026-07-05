package com.fitness.activityservice.service;

import java.util.List;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;

public interface ActivityService {

	
	ActivityResponse trackActivity(ActivityRequest request);

	List<ActivityResponse> getUserActivities(String userId);

	ActivityResponse getActivityById(String activityId);

}
