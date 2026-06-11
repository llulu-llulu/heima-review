package com.hmdp.config;

import com.hmdp.utils.RefreshTokenIntercepter;
import com.hmdp.utils.loginIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private final StringRedisTemplate stringRedisTemplate;
    public MvcConfig(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(new loginIntercepter())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/code",
                        "/blog/hot",
                        "/upload/**",
                        "/shop-type/**",
                        "/voucher/**",
                        "/shop/**"
                ).order(1);
        //刷新token拦截器
        registry.addInterceptor(new RefreshTokenIntercepter(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
