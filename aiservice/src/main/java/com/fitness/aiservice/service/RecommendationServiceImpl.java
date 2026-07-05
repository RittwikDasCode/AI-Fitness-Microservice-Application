package com.fitness.aiservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService{

	@Autowired
	private final RecommendationRepository recommendationRepository;
	
	@Override
	public List<Recommendation> getUserRecommendation(String userId) {
		return recommendationRepository.findByUserId(userId);
	}

	@Override
	public Recommendation getActivityRecommendation(String activityId) {
		return recommendationRepository.findByActivityId(activityId)
				.orElseThrow(() -> new RuntimeException("No recommendation found with the activity: " + activityId));
	}

}
