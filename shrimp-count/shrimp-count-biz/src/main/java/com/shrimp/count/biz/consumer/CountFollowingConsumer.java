package com.shrimp.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.enums.FollowUnFollowTypeEnum;
import com.shrimp.count.biz.model.dto.CountFollowUnfollowMQDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
@Slf4j
public class CountFollowingConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;


    @Override
    public void onMessage(String message) {
        log.info("## 消费到了 MQ 【计数: 关注数】, {}...", message);
        if (StringUtils.isBlank(message))
            return;

        CountFollowUnfollowMQDTO mqDto = JsonUtils.parseJSONString(message, CountFollowUnfollowMQDTO.class);
        FollowUnFollowTypeEnum typeEnum = FollowUnFollowTypeEnum.valueOf(mqDto.getType());
        if (typeEnum == null)
            return;

        Long userId = mqDto.getUserId();
        int count = 0;
        switch (typeEnum) {
            case FOLLOW -> count += 1;
            case UNFOLLOW -> count -= 1;
        }

        String userRedisKey = RedisKeyConstants.buildCountUserKey(userId);
        Boolean hasKey = redisTemplate.hasKey(userRedisKey);
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.opsForHash().increment(userRedisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
        }

//        发送MQ
        Message<String> sendMessage = MessageBuilder.withPayload(message)
                .build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, sendMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
