package com.shrimp.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.enums.CollectUnCollectTypeEnum;
import com.shrimp.count.biz.model.dto.AggregationCountCollectUncollectedNoteMqDTO;
import com.shrimp.count.biz.model.dto.CollectUnCollectNoteMqDTO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
@Slf4j
public class CountCollectUncollectedNoteComsumer implements RocketMQListener<String> {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
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
        log.info("##count服务消费收藏消息：messages:{}", messages);
//       将message转化成dto
        List<CollectUnCollectNoteMqDTO> collectUnCollectNoteMqDTOS = messages.stream()
                .map(message -> JsonUtils.parseJSONString(message, CollectUnCollectNoteMqDTO.class))
                .toList();
//       dto->map<Long,List>
        Map<Long, List<CollectUnCollectNoteMqDTO>> mapByNoteId = collectUnCollectNoteMqDTOS.stream().collect(Collectors.groupingBy(CollectUnCollectNoteMqDTO::getNoteId));

        List<AggregationCountCollectUncollectedNoteMqDTO> countList = new ArrayList<>(mapByNoteId.size());

        for (Map.Entry<Long, List<CollectUnCollectNoteMqDTO>> entry : mapByNoteId.entrySet()) {
            List<CollectUnCollectNoteMqDTO> list = entry.getValue();
            Long noteId = entry.getKey();
            Long noteCreatorId = null;
            int finalCount = 0;

            for (CollectUnCollectNoteMqDTO dto : list) {
                noteCreatorId = dto.getNoteCreatorId();
                if (CollectUnCollectTypeEnum.COLLECT.getCode().equals(dto.getType())) {
                    finalCount += 1;
                } else if (CollectUnCollectTypeEnum.UNCOLLECTED.getCode().equals(dto.getType())) {
                    finalCount -= 1;
                }
            }

            countList.add(AggregationCountCollectUncollectedNoteMqDTO.builder()
                    .noteId(noteId)
                    .noteCreatorId(noteCreatorId)
                    .count(finalCount)
                    .build()
            );
        }
//         遍历countList
        for (AggregationCountCollectUncollectedNoteMqDTO agg : countList) {
            Long noteId = agg.getNoteId();
            Long noteCreatorId = agg.getNoteCreatorId();
            Integer count = agg.getCount();
//            note方面
            String countNoteKey = RedisKeyConstants.buildCountNoteKey(noteId);
            Boolean hasKey = redisTemplate.hasKey(countNoteKey);
//        如果RedisKey存在，则更新
            if (Boolean.TRUE.equals(hasKey)) {
                redisTemplate.opsForHash().increment(countNoteKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, count);
            }

//            笔记创建者方面
            String countUserKey = RedisKeyConstants.buildCountUserKey(noteCreatorId);
            hasKey = redisTemplate.hasKey(countUserKey);
//        如果RedisKey存在，则更新
            if (Boolean.TRUE.equals(hasKey)) {
                redisTemplate.opsForHash().increment(countUserKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, count);
            }
        }

//       发送MQ落库
        String mapStr = JsonUtils.toJsonString(countList);
        Message<String> message = MessageBuilder.withPayload(mapStr).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记收藏数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：笔记收藏数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
