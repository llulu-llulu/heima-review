package com.hmdp.contract;

import com.hmdp.ai.client.impl.MockLlmClient;
import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MockLlmClientTest {

    @Test
    public void mockClientShouldReturnLocalResponseWithoutApiKey() {
        LlmRequest request = new LlmRequest();
        request.setScene("merchant-copywriting");
        request.setPrompt("请生成一段商家文案");

        LlmResponse response = new MockLlmClient().chat(request);

        assertTrue(response.getSuccess());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("Mock"));
        assertTrue(response.getEstimatedTokens() > 0);
    }
}
