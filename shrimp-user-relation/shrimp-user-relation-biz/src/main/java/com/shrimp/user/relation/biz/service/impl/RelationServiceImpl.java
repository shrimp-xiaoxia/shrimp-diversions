package com.shrimp.user.relation.biz.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.user.relation.biz.constants.MQConstants;
import com.shrimp.user.relation.biz.constants.RedisKeyConstants;
import com.shrimp.user.relation.biz.domain.dataobject.FollowingDO;
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.biz.enums.LuaResultEnum;
import com.shrimp.user.relation.biz.enums.ResponseCodeEnum;
import com.shrimp.user.relation.biz.model.dto.FollowUserMQDTO;
import com.shrimp.user.relation.biz.model.vo.FollowUserReqVO;
import com.shrimp.user.relation.biz.rpc.UserRpcService;
import com.shrimp.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author ccb
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserRpcService userRpcService;
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final static DefaultRedisScript<Long> FOLLOW_CHECK_AND_ADD_SCRIPT;

    static {
        FOLLOW_CHECK_AND_ADD_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua"));
        FOLLOW_CHECK_AND_ADD_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_CHECK_AND_ADD_SCRIPT.setResultType(Long.class);
    }

    private final static DefaultRedisScript<Long> FOLLOW_ADD_AND_EXPIRE_SCRIPT;

    static {
        FOLLOW_ADD_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua"));
        FOLLOW_ADD_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_ADD_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private final static DefaultRedisScript<Long> FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT;

    static {
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua"));
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Response<?> followUser(FollowUserReqVO followUserReqVO) {
//          获取关注用户ID
        Long followUserId = followUserReqVO.getFollowUserId();
//          获取当前用户ID
        Long loginUserId = LoginUserContextHolder.getLoginUserId();
//          若关注的用户是自己，则抛出异常
        if (ObjUtil.equals(loginUserId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }
//          校验关注的用户是否存在
        boolean flag = userRpcService.checkUserIsExist(followUserId);
        if (!flag) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

//          执行redis的Lua脚本
        String key = RedisKeyConstants.buildFollowUserZsetKey(loginUserId);
        LocalDateTime createTime = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(createTime);

        Long result = redisTemplate.execute(FOLLOW_CHECK_AND_ADD_SCRIPT, Collections.singletonList(key), followUserId, timestamp);

        checkLuaScriptResult(result);

        if (LuaResultEnum.valueOf(result).equals(LuaResultEnum.ZSET_NOT_EXIST)) {
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserid(loginUserId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            if (followingDOS == null || followingDOS.isEmpty()) {
                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策略
                redisTemplate.execute(FOLLOW_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), followUserId, timestamp, expireSeconds);
            } else {
                Object[] objects = buildArgs(followingDOS, expireSeconds);
                redisTemplate.execute(FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), objects);

                result = redisTemplate.execute(FOLLOW_CHECK_AND_ADD_SCRIPT, Collections.singletonList(key), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }
        // 发送 MQ
//        构建消息对象
        FollowUserMQDTO followUserMQDTO = FollowUserMQDTO.builder()
                .userId(loginUserId)
                .followUserId(followUserId)
                .createTime(createTime)
                .build();
//        转换为JSON格式
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMQDTO)).build();
// 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;
        log.info("==> 开始发送关注操作 MQ, 消息体: {}", followUserMQDTO);
//        异步发送消息给消费端，提高接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    private void checkLuaScriptResult(Long luaResult) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(luaResult);
        if (Objects.isNull(luaResultEnum)) {
            throw new RuntimeException("Lua 返回结果错误");
        }

        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    private Object[] buildArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int listSize = followingDOS.size();
        Object[] args = new Object[2 * listSize + 1];
        int index = 0;
        for (FollowingDO followingDO : followingDOS) {
            args[index] = followingDO.getCreateTime();
            args[index + 1] = followingDO.getFollowingUserId();
            index += 2;
        }
        args[args.length - 1] = expireSeconds;
        return args;
    }
}
