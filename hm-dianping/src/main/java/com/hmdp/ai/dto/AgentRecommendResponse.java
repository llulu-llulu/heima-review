package com.hmdp.ai.dto;

import com.hmdp.entity.Shop;
import lombok.Data;

import java.util.List;

@Data
public class AgentRecommendResponse {
    private String answer;
    private List<Shop> shops;
    private List<String> reasons;
    private List<String> usedTools;
    private List<String> references;
    private Boolean needMoreInfo;
}
