package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenIntercepter implements HandlerInterceptor {

    @Resource
    private final StringRedisTemplate stringRedisTemplate;
    public RefreshTokenIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取session
        HttpSession session = request.getSession();
        //获取token
        String token = request.getHeader("authorization");
        //校验token是否为空
        if (StrUtil.isBlank(token)) {
            //用户未登录，跳转到登录页
            return false;
        }

        String userKey = LOGIN_USER_KEY + token;
        //校验用户是否登录
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);
        if (userMap.isEmpty()) {
            //用户未登录，跳转到登录页
            return false;
        }

        //将userMap转换为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //用户已登录，放行
        UserHolder.saveUser(userDTO);
        //设置有效期
        stringRedisTemplate.expire(userKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
}
