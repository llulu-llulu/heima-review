package com.hmdp.contract;

import com.hmdp.ai.controller.AiAgentController;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AiAgentContractTest {

    @Test
    public void agentMappingShouldUseAiPrefix() throws Exception {
        RequestMapping base = AiAgentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(base);
        assertTrue(Arrays.asList(base.value()).contains("/ai/agent"));

        Method method = AiAgentController.class.getDeclaredMethod("recommendShop", com.hmdp.ai.dto.AgentRecommendRequest.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/recommend-shop"));
    }
}
