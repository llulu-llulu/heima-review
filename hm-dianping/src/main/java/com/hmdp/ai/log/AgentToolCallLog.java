package com.hmdp.ai.log;

import lombok.Data;

@Data
public class AgentToolCallLog {
    private String toolName;
    private String arguments;
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
}
