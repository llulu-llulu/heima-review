package com.hmdp.ai.log;

import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class AiCallLogger {

    public AiCallLog log(LlmRequest request, LlmResponse response) {
        AiCallLog callLog = new AiCallLog();
        callLog.setRequestId(UUID.randomUUID().toString());
        callLog.setScene(request == null ? "unknown" : request.getScene());
        callLog.setModel(response == null ? null : response.getModel());
        callLog.setLatencyMs(response == null ? null : response.getLatencyMs());
        callLog.setSuccess(response != null && Boolean.TRUE.equals(response.getSuccess()));
        callLog.setErrorMessage(response == null ? "empty response" : response.getErrorMessage());
        callLog.setEstimatedTokens(response == null ? 0L : response.getEstimatedTokens());
        callLog.setCreateTime(LocalDateTime.now());
        log.info("ai_call requestId={}, scene={}, model={}, latencyMs={}, success={}, estimatedTokens={}",
                callLog.getRequestId(),
                callLog.getScene(),
                callLog.getModel(),
                callLog.getLatencyMs(),
                callLog.getSuccess(),
                callLog.getEstimatedTokens());
        return callLog;
    }
}
