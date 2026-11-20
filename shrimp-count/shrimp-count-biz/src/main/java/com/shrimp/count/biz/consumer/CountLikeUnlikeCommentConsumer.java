package com.shrimp.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.enums.LikeUnlikeCommentTypeEnum;
import com.shrimp.count.biz.model.dto.AggregationCountLikeUnlikeCommentMqDTO;
import com.shrimp.count.biz.model.dto.LikeUnlikeCommentMqDTO;
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

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_count_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE
)
@Slf4j
public class CountLikeUnlikeCommentConsumer implements RocketMQListener<String> {
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(5000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String s) {
        bufferTrigger.enqueue(s);
    }

    private void consumeMessage(List<String> messages) {
//          打印日记
        log.info("==> 【评论点赞数】聚合消息, size: {}", messages.size());
        log.info("==> 【评论点赞数】聚合消息, {}", JsonUtils.toJsonString(messages));
//        转化成MQ消息体
        List<LikeUnlikeCommentMqDTO> mqDTOList = messages.stream().map(message ->
                JsonUtils.parseJSONString(message, LikeUnlikeCommentMqDTO.class)).toList();
//        转化成IdAndListMap
        Map<Long, List<LikeUnlikeCommentMqDTO>> commentIdAndListMap = mqDTOList.stream().collect(Collectors.groupingBy(LikeUnlikeCommentMqDTO::getCommentId));
//        聚合数据
        for (Map.Entry<Long, List<LikeUnlikeCommentMqDTO>> entry : commentIdAndListMap.entrySet()) {
            //        遍历map，累加其值
            Long commentId = entry.getKey();
            int count = 0;
            List<LikeUnlikeCommentMqDTO> likeUnlikeList = entry.getValue();
            for (LikeUnlikeCommentMqDTO dto : likeUnlikeList) {
                Integer type = dto.getType();
                if (LikeUnlikeCommentTypeEnum.LIKE.getCode().equals(type)) {
                    count += 1;
                }
                if (LikeUnlikeCommentTypeEnum.UNLIKE.getCode().equals(type)) {
                    count -= 1;
                }
            }
//         更新Redis缓存评论计数
            String commentCountKey = RedisKeyConstants.buildCountCommentKey(commentId);
            Boolean hasKey = redisTemplate.hasKey(commentCountKey);
            if (hasKey) {
                redisTemplate.opsForHash().increment(commentCountKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }

//        发送Mq，落库
            AggregationCountLikeUnlikeCommentMqDTO aggregationCountLikeUnlikeCommentMqDTO = AggregationCountLikeUnlikeCommentMqDTO.builder()
                    .commentId(commentId)
                    .count(count)
                    .build();
            Message<String> aggregationMessage = MessageBuilder.withPayload(JsonUtils.toJsonString(aggregationCountLikeUnlikeCommentMqDTO)).build();

            rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE_2_DB, aggregationMessage, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> 【计数服务：评论点赞数写库】MQ 发送成功，SendResult: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> 【计数服务：评论点赞数写库】MQ 发送异常: ", throwable);
                }
            });

        }
    }


}
