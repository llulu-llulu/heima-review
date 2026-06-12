package com.hmdp.ai.service;

import com.hmdp.ai.client.LlmClient;
import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import com.hmdp.ai.log.AiCallLogger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LlmService {

    private static final String FALLBACK_CONTENT = "AI 服务暂时不可用，请稍后重试。";

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiCallLogger aiCallLogger;

    public LlmResponse chat(LlmRequest request) {
        LlmResponse response;
        try {
            response = llmClient.chat(request);
        } catch (Exception e) {
            response = LlmResponse.fallback(FALLBACK_CONTENT, e.getMessage());
        }
        aiCallLogger.log(request, response);
        return response;
    }
}
