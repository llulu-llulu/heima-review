package com.hmdp.contract;

import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.RefreshTokenIntercepter;
import org.junit.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EngineeringRegressionTest {

    @Test
    public void refreshTokenInterceptorShouldAllowAnonymousRequests() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RefreshTokenIntercepter interceptor = new RefreshTokenIntercepter(redisTemplate);

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new Object()
        );

        assertTrue(allowed);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    public void refreshTokenInterceptorShouldAllowInvalidTokenAndLeaveLoginInterceptorToRejectProtectedApi() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("login:token:bad-token")).thenReturn(Collections.emptyMap());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("authorization", "bad-token");

        RefreshTokenIntercepter interceptor = new RefreshTokenIntercepter(redisTemplate);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    public void redisIdWorkerShouldUseEpochSecondsAndKeepSequenceInLowBits() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(7L);

        RedisIdWorker worker = new RedisIdWorker(redisTemplate);
        long id = worker.nextId("order");

        assertTrue("id should be positive after using epoch seconds", id > 0);
        assertEquals(7L, id & ((1L << 32) - 1));
    }
}
