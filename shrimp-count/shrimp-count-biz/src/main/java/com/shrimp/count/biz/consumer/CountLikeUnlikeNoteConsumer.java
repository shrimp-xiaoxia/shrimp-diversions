package com.shrimp.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.enums.LikeUnlikeTypeEnum;
import com.shrimp.count.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
@Slf4j
public class CountLikeUnlikeNoteConsumer implements RocketMQListener<String> {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
        log.info("##计数服务接收到消息：{}", messages);
//        将数据转化为mqDTO
        List<LikeUnlikeNoteMqDTO> likeUnlikeNoteMqDTOS = messages.stream().map(message ->
                JsonUtils.parseJSONString(message, LikeUnlikeNoteMqDTO.class)
        ).toList();
//        将list转化为map<Long,MqDTO>
        Map<Long, List<LikeUnlikeNoteMqDTO>> mapByNoteId = likeUnlikeNoteMqDTOS.stream()
                .collect(Collectors.groupingBy(LikeUnlikeNoteMqDTO::getNoteId));
//        mqDTO转换成Map<Long,Integer>
        HashMap<Long, Integer> countMap = Maps.newHashMap();
        for (Map.Entry<Long, List<LikeUnlikeNoteMqDTO>> entry : mapByNoteId.entrySet()) {
            List<LikeUnlikeNoteMqDTO> list = entry.getValue();
            Long key = entry.getKey();
            int count = 0;
            for (LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO : list) {
                if (LikeUnlikeTypeEnum.LIKE.getCode().equals(likeUnlikeNoteMqDTO.getType())) {
                    count += 1;
                } else if (LikeUnlikeTypeEnum.UNLIKE.getCode().equals(likeUnlikeNoteMqDTO.getType())) {
                    count -= 1;
                }
            }
            countMap.put(key, count);
        }
//        遍历Map
        countMap.forEach((k, v) -> {
            //        构建Redis的key
            String countNoteKey = RedisKeyConstants.buildCountNoteKey(k);
//        如果对应的key不存在，则不进行修改
            Boolean hasKey = redisTemplate.hasKey(countNoteKey);
            // 若存在才会更新
            // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
            if (Boolean.TRUE.equals(hasKey)) {
                redisTemplate.opsForHash().increment(countNoteKey, RedisKeyConstants.FIELD_LIKE_TOTAL, v);
            }
//        存在则进行需改
        });

//       TODO 发送MQ到数据库落库端
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap)).build();
        String topicCountNoteLike2Db = MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB;

        rocketMQTemplate.asyncSend(topicCountNoteLike2Db, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记点赞数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：笔记点赞数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
