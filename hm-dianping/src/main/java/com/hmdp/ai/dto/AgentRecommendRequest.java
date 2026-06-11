package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class AgentRecommendRequest {
    private String query;
    private Long userId;
    private Double x;
    private Double y;
    private Long typeId;
    private Integer maxCandidates;
}
