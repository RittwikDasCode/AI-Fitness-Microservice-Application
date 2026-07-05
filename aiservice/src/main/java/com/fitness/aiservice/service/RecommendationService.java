package com.fitness.aiservice.service;

import java.util.List;

import com.fitness.aiservice.model.Recommendation;

public interface RecommendationService {

	List<Recommendation> getUserRecommendation(String userId);

	Recommendation getActivityRecommendation(String activityId);

}
