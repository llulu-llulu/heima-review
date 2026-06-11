package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class LlmRequest {
    private String scene;
    private String prompt;
    private String systemPrompt;
    private String userPrompt;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}
