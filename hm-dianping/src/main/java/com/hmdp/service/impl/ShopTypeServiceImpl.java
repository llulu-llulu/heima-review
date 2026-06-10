package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY = "shop:type:list";

    @Override
    public List<ShopType> queryTypeList() {
        // 先从 Redis 查询缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (json != null) {
            // 缓存命中，直接返回
            return JSONUtil.toList(json, ShopType.class);
        }

        // 缓存未命中，从数据库查询
        List<ShopType> typeList = query().orderByAsc("sort").list();

        // 将结果写入 Redis，设置过期时间 10 分钟
        stringRedisTemplate.opsForValue().set(CACHE_KEY, JSONUtil.toJsonStr(typeList), 10, TimeUnit.MINUTES);

        return typeList;
    }
}