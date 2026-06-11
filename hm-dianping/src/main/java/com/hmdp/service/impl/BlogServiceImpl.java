package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());

        // 先保存笔记，拿到数据库生成的 blogId 后才能推送给粉丝。
        boolean success = save(blog);
        if (!success) {
            return Result.fail("发布笔记失败");
        }

        // 推模式：把新笔记 id 写入每个粉丝的收件箱，score 使用发布时间戳。
        List<Follow> fans = followService.query().eq("follow_user_id", user.getId()).list();
        long now = System.currentTimeMillis();
        for (Follow fan : fans) {
            String key = FEED_KEY + fan.getUserId();
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), now);
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;

        // 滚动分页：按时间倒序读取收件箱；offset 用来跳过同一时间戳下已读过的数据。
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, SystemConstants.MAX_PAGE_SIZE);
        if (tuples == null || tuples.isEmpty()) {
            ScrollResult scrollResult = new ScrollResult();
            scrollResult.setList(Collections.emptyList());
            scrollResult.setMinTime(0L);
            scrollResult.setOffset(0);
            return Result.ok(scrollResult);
        }

        // 记录本页 blogId，并计算下一次滚动分页要传回的 minTime 和 offset。
        List<Long> ids = new ArrayList<>(tuples.size());
        long minTime = 0;
        int nextOffset = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                nextOffset++;
            } else {
                minTime = time;
                nextOffset = 1;
            }
        }

        // 逐个查询可以保持 feed 中的时间倒序；单页数量较小，避免 listByIds 打乱顺序。
        List<Blog> blogs = ids.stream()
                .map(this::getById)
                .filter(blog -> blog != null)
                .collect(Collectors.toList());
        blogs.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });

        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(nextOffset);
        return Result.ok(scrollResult);
    }

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        if (userId == null) {
            return;
        }
        String key = BLOG_LIKED_KEY + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString()) != null;
        blog.setIsLike(BooleanUtil.isTrue(isMember));
    }

    @Override
    public Result queryHotBlog(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }

        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            Boolean updateResult = update().setSql("liked = liked + 1").eq("id", id).update();
            if (updateResult) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            Boolean updateResult = update().setSql("liked = liked - 1").eq("id", id).update();
            if (updateResult) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }

        return Result.ok(blog);
    }

    @Override
    public Result queryLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> members = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        List<UserDTO> userDTOS = members.stream().map(userId -> {
            User user = userService.getById(Long.parseLong(userId));
            return BeanUtil.copyProperties(user, UserDTO.class);
        }).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
