package com.shrimp.note.biz.controller;

import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.domain.mapper.ChannelDOMapper;
import com.shrimp.note.biz.domain.mapper.TopicDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.TopicCreateReqVO;
import com.shrimp.note.biz.model.vo.req.TopicSearchReqVO;
import com.shrimp.note.biz.model.vo.rsp.NamedItemRspVO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class ChannelTopicController {
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    @Resource private ChannelDOMapper channelMapper;
    @Resource private TopicDOMapper topicMapper;
    @Resource private StringRedisTemplate redis;

    @PostMapping("/channel/list")
    public Response<List<NamedItemRspVO>> channels() {
        return Response.success(channelMapper.selectEnabled().stream()
                .map(item -> new NamedItemRspVO(String.valueOf(item.getId()), item.getName())).toList());
    }

    @PostMapping("/topic/list")
    public Response<List<NamedItemRspVO>> topics(@Validated @RequestBody TopicSearchReqVO request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        return Response.success(topicMapper.searchEnabled(keyword).stream()
                .map(item -> new NamedItemRspVO(String.valueOf(item.getId()), item.getName())).toList());
    }

    @PostMapping("/topic/create")
    public Response<?> create(@Validated @RequestBody TopicCreateReqVO request) {
        if (LoginUserContextHolder.getLoginUserId() == null) throw new BizException(ResponseCodeEnum.LOGIN_REQUIRED);
        String name = request.getName() == null ? "" : request.getName().strip();
        if (name.isEmpty()) throw new BizException(ResponseCodeEnum.TOPIC_NAME_EMPTY);
        if (name.codePointCount(0, name.length()) > 12) throw new BizException(ResponseCodeEnum.TOPIC_NAME_TOO_LONG);
        if (name.codePoints().anyMatch(Character::isWhitespace)) throw new BizException(ResponseCodeEnum.TOPIC_NAME_INVALID);
        String key = "topic:create:" + name;
        String owner = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, 30, TimeUnit.SECONDS))) {
            throw new BizException(ResponseCodeEnum.TOPIC_CREATE_BUSY);
        }
        try {
            if (topicMapper.findEnabledByName(name) != null) throw new BizException(ResponseCodeEnum.TOPIC_ALREADY_EXISTS);
            topicMapper.insertTopic(name);
            return Response.success();
        } finally {
            redis.execute(RELEASE_LOCK, Collections.singletonList(key), owner);
        }
    }
}
