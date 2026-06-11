package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_KEY;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if (userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }

        String key = FOLLOW_KEY + userId;
        if (Boolean.TRUE.equals(isFollow)) {
            // 数据库保存真实关注关系；Redis Set 保存关注用户 id，便于共同关注取交集。
            int count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
            if (count == 0) {
                Follow follow = new Follow();
                follow.setUserId(userId);
                follow.setFollowUserId(followUserId);
                boolean success = save(follow);
                if (!success) {
                    return Result.fail("关注失败");
                }
            }
            stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            return Result.ok();
        }

        // 取关时同时删除数据库关系和 Redis Set 中的缓存，避免共同关注结果出现脏数据。
        remove(query().eq("user_id", userId).eq("follow_user_id", followUserId));
        stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long followUserId) {
        Long userId = UserHolder.getUser().getId();

        // 两个用户的关注列表都保存在 Redis Set，SINTER 可以直接得到共同关注的用户 id。
        String key = FOLLOW_KEY + userId;
        String followUserKey = FOLLOW_KEY + followUserId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, followUserKey);

        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
