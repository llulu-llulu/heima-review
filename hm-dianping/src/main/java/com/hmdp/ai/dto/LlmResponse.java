package com.hmdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    private String content;
    private Boolean success;
    private String errorMessage;
    private String model;
    private Long latencyMs;
    private Long estimatedTokens;

    public static LlmResponse success(String content, String model, long latencyMs, long estimatedTokens) {
        return new LlmResponse(content, true, null, model, latencyMs, estimatedTokens);
    }

    public static LlmResponse fallback(String content, String errorMessage) {
        return new LlmResponse(content, false, errorMessage, "fallback", 0L, 0L);
    }
}
