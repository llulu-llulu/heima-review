package com.hmdp.ai.client.impl;

import com.hmdp.ai.client.LlmClient;
import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {

    private static final String MODEL_NAME = "mock-local-llm";

    @Override
    public LlmResponse chat(LlmRequest request) {
        long start = System.currentTimeMillis();
        String prompt = mergePrompt(request);
        String content = buildMockContent(prompt);
        long latency = System.currentTimeMillis() - start;
        return LlmResponse.success(content, MODEL_NAME, latency, estimateTokens(prompt, content));
    }

    private String mergePrompt(LlmRequest request) {
        StringBuilder builder = new StringBuilder();
        append(builder, request.getSystemPrompt());
        append(builder, request.getPrompt());
        append(builder, request.getUserPrompt());
        return builder.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value.trim());
        }
    }

    private String buildMockContent(String prompt) {
        String lower = prompt == null ? "" : prompt.toLowerCase();
        if (lower.contains("copywriting") || prompt.contains("文案")) {
            return "【Mock】已基于店铺卖点生成营销文案，建议突出核心菜品、优惠力度和到店场景。";
        }
        if (lower.contains("review") || prompt.contains("评论")) {
            return "【Mock】评论整体偏正向，用户关注环境、服务和性价比，建议持续优化排队体验。";
        }
        if (lower.contains("rag") || prompt.contains("问答")) {
            return "【Mock】已根据检索到的店铺资料回答，并保留 references 作为依据。";
        }
        if (lower.contains("agent") || prompt.contains("推荐")) {
            return "【Mock】已调用候选店铺检索和排序工具，给出可解释推荐结果。";
        }
        return "【Mock】本地模拟模型已返回结果，当前未调用任何外部大模型服务。";
    }

    private long estimateTokens(String prompt, String content) {
        int promptLength = prompt == null ? 0 : prompt.length();
        int contentLength = content == null ? 0 : content.length();
        return Math.max(1, (promptLength + contentLength) / 2);
    }
}
