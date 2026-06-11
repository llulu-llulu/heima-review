package com.hmdp.ai.controller;

import com.hmdp.ai.dto.AgentRecommendRequest;
import com.hmdp.ai.service.RecommendationAgentService;
import com.hmdp.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/agent")
public class AiAgentController {

    private final RecommendationAgentService recommendationAgentService;

    public AiAgentController(RecommendationAgentService recommendationAgentService) {
        this.recommendationAgentService = recommendationAgentService;
    }

    @PostMapping("/recommend-shop")
    public Result recommendShop(@RequestBody AgentRecommendRequest request) {
        if (request == null || recommendationAgentService.invalidQuery(request.getQuery())) {
            return Result.fail("query不能为空");
        }
        return Result.ok(recommendationAgentService.recommend(request));
    }
}
