package com.hmdp.ai.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptTemplateService {

    public String load(String templateName) {
        ClassPathResource resource = new ClassPathResource("prompts/" + templateName + ".txt");
        if (!resource.exists()) {
            return "";
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
