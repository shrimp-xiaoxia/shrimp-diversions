package com.shrimp.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.enums.FollowUnFollowTypeEnum;
import com.shrimp.count.biz.model.dto.CountFollowUnfollowMQDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_FANS,
        topic = MQConstants.TOPIC_COUNT_FANS
)
@Slf4j
public class CountFansConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(5000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();


    @Override
    public void onMessage(String message) {
        bufferTrigger.enqueue(message);
    }

    private void consumeMessage(List<String> messages) {
        log.info("==> 聚合消息, size: {}", messages.size());
        log.info("==> 聚合消息, {}", JsonUtils.toJsonString(messages));
//        转换成MqDTO形式
        List<CountFollowUnfollowMQDTO> countFollowUnfollowMQDTOS = messages.stream()
                .map(message -> JsonUtils.parseJSONString(message, CountFollowUnfollowMQDTO.class))
                .toList();
//        根据targetUserID来分组
        Map<Long, List<CountFollowUnfollowMQDTO>> mapByTargetId = countFollowUnfollowMQDTOS.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowMQDTO::getTargetUserId));
//        将map中的数据聚合处理，转换成map<Long,Integer>
        // key 为目标用户ID, value 为最终操作的计数
        Map<Long, Integer> countMap = Maps.newHashMap();
        for (Map.Entry<Long, List<CountFollowUnfollowMQDTO>> entry : mapByTargetId.entrySet()) {
            List<CountFollowUnfollowMQDTO> list = entry.getValue();

            int finalCount = 0;
            for (CountFollowUnfollowMQDTO mqDto : list) {
                Integer type = mqDto.getType();
                FollowUnFollowTypeEnum typeEnum = FollowUnFollowTypeEnum.valueOf(type);
                if (ObjUtil.isNull(typeEnum))
                    continue;
                if (typeEnum.equals(FollowUnFollowTypeEnum.FOLLOW)) {
                    finalCount += 1;
                } else if (typeEnum.equals(FollowUnFollowTypeEnum.UNFOLLOW)) {
                    finalCount -= 1;
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }

        log.info("## 聚合后的计数数据: {}", JsonUtils.toJsonString(countMap));
//        遍历map，如果存在对应的key，则执行更新操作
        for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
            String countRedisKey = RedisKeyConstants.buildCountUserKey(entry.getKey());
            Integer totalCount = entry.getValue();

            Boolean hasKey = redisTemplate.hasKey(countRedisKey);
            if (Boolean.TRUE.equals(hasKey)) {
                redisTemplate.opsForHash().increment(countRedisKey, RedisKeyConstants.FIELD_FANS_TOTAL, totalCount);
            }
        }

        // 发送 MQ, 计数数据落库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap))
                .build();

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
