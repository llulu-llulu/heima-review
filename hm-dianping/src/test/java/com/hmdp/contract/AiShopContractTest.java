package com.hmdp.contract;

import com.hmdp.ai.controller.AiShopController;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AiShopContractTest {

    @Test
    public void shopAiMappingsShouldUseAiPrefix() throws Exception {
        RequestMapping base = AiShopController.class.getAnnotation(RequestMapping.class);
        assertNotNull(base);
        assertTrue(Arrays.asList(base.value()).contains("/ai/shop"));
        assertPost("ask", "/ask");
        assertPost("reindex", "/reindex");
    }

    private void assertPost(String methodName, String path) throws Exception {
        Method method = Arrays.stream(AiShopController.class.getDeclaredMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing method " + methodName));
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains(path));
    }
}
