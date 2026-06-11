package com.hmdp.ai.client;

import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);

    default String generate(String prompt) {
        LlmRequest request = new LlmRequest();
        request.setPrompt(prompt);
        LlmResponse response = chat(request);
        return response.getContent();
    }
}
