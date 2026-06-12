package com.hmdp.ai.controller;

import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import com.hmdp.ai.service.LlmService;
import com.hmdp.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ai/llm")
public class AiLlmController {

    @Resource
    private LlmService llmService;

    @PostMapping("/chat")
    public Result chat(@RequestBody LlmRequest request) {
        LlmResponse response = llmService.chat(request);
        return Result.ok(response);
    }
}
