package com.shrimp.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.domain.mapper.CommentDOMapper;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.biz.enums.CommentLevelEnum;
import com.shrimp.count.biz.model.dto.CountPublishCommentMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_child_comment_total" + MQConstants.TOPIC_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT
)
@Slf4j
public class CountNoteChildCommentConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;
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
    public void onMessage(String messages) {
        bufferTrigger.enqueue(messages);
    }

    private void consumeMessage(List<String> messages) {
        log.info("==> 【笔记二级评论数】聚合消息, size: {}", messages.size());
        log.info("==> 【笔记二级评论数】聚合消息, {}", JsonUtils.toJsonString(messages));
//    转换成dto
        List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();
        messages.forEach(message -> {
            try {
                List<CountPublishCommentMqDTO> countPublishCommentMqDTOS = JsonUtils.parseList(message, CountPublishCommentMqDTO.class);
                countPublishCommentMqDTOList.addAll(countPublishCommentMqDTOS);
            } catch (Exception e) {
                log.error("", e);
            }
        });
//        分组
        Map<Long, List<CountPublishCommentMqDTO>> mapByParentId = countPublishCommentMqDTOList.stream()
                .filter(dto -> ObjUtil.equals(dto.getLevel(), CommentLevelEnum.TWO.getCode()))
                .collect(Collectors.groupingBy(CountPublishCommentMqDTO::getParentId));

        if (CollUtil.isEmpty(mapByParentId)) {
            return;
        }

//        插入数据
        mapByParentId.entrySet().stream().forEach(entry -> {
            Long commentId = entry.getKey();
            int childCommentTotal = entry.getValue().size();
            if (childCommentTotal > 0) {
                //更新Redis缓存中的评论计数数据
                //构建key
                String countChildCommentKey = RedisKeyConstants.buildCountCommentKey(commentId);
                Boolean hasKey = redisTemplate.hasKey(countChildCommentKey);
                if (hasKey) {
                    redisTemplate.opsForHash()
                            .increment(countChildCommentKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, childCommentTotal);
                }
                commentDOMapper.updateChildCommentTotalInt(commentId, childCommentTotal);
            }
        });

        Set<Long> commentIds = mapByParentId.keySet();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentIds)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论热度值更新】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论热度值更新】MQ 发送异常: ", throwable);
            }
        });
    }
}
