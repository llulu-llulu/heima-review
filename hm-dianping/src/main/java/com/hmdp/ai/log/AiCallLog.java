package com.hmdp.ai.log;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLog {
    private String requestId;
    private String scene;
    private String model;
    private Long latencyMs;
    private Boolean success;
    private String errorMessage;
    private Long estimatedTokens;
    private LocalDateTime createTime;
}
