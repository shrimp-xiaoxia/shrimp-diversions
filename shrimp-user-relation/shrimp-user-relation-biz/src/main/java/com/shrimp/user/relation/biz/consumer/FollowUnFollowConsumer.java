package com.shrimp.user.relation.biz.consumer;

import cn.hutool.core.util.ObjUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.user.relation.biz.constants.MQConstants;
import com.shrimp.user.relation.biz.constants.RedisKeyConstants;
import com.shrimp.user.relation.biz.domain.dataobject.FansDO;
import com.shrimp.user.relation.biz.domain.dataobject.FollowingDO;
import com.shrimp.user.relation.biz.domain.mapper.FansDOMapper;
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.biz.model.dto.FollowUserMQDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW
)
@Slf4j
public class FollowUnFollowConsumer implements RocketMQListener<Message> {

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final static DefaultRedisScript<Long> FOLLOW_CHECK_AND_UPDATE_FANS_ZSET_SCRIPT;

    static {
        FOLLOW_CHECK_AND_UPDATE_FANS_ZSET_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua"));
        FOLLOW_CHECK_AND_UPDATE_FANS_ZSET_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_CHECK_AND_UPDATE_FANS_ZSET_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        if (ObjUtil.equals(tags, MQConstants.TAG_FOLLOW)) {
            handleFollowTagMessage(bodyJsonStr);
        } else if (ObjUtil.equals(tags, MQConstants.TAG_UNFOLLOW)) {
//            TODO
        }

    }

    private void handleFollowTagMessage(String bodyJsonStr) {
        FollowUserMQDTO followUserMqDTO = JsonUtils.parseJSONString(bodyJsonStr, FollowUserMQDTO.class);
        if (ObjUtil.isNull(followUserMqDTO)) {
            return;
        }
        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();
//        开始编程式事务
        boolean flag = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                followingDOMapper.insert(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());

                fansDOMapper.insert(FansDO.builder()
                        .userId(followUserId)
                        .fansUserId(userId)
                        .createTime(createTime)
                        .build());
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("", e);
            }
            return false;
        }));

        log.info("==>数据库添加结果：{}", flag);
        if (flag) {
            //更新 Redis 中被关注用户的 ZSet 粉丝列表
            String key = RedisKeyConstants.buildUserFansZsetKey(followUserId);
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);
            Long result = redisTemplate.execute(FOLLOW_CHECK_AND_UPDATE_FANS_ZSET_SCRIPT, Collections.singletonList(key), userId, timestamp);
            log.info("==>该被关注者的zset初始化情况：{}", result == 0);
        }

    }
}
