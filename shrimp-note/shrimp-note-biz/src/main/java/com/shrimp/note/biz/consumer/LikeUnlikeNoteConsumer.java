package com.shrimp.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.domain.dataobject.NoteLikeDO;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.enums.LikeUnlikeTypeEnum;
import com.shrimp.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE, consumeMode = ConsumeMode.ORDERLY)
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tag = message.getTags();
        log.info("==> 笔记点赞关系消费消息: {}, tag: {}", body, tag);

        LikeUnlikeNoteMqDTO dto = JsonUtils.parseJSONString(body, LikeUnlikeNoteMqDTO.class);
        if (dto == null) {
            throw new IllegalArgumentException("笔记点赞消息内容为空");
        }
        int affected;
        if (MQConstants.TAG_LIKE.equals(tag)) {
            NoteLikeDO record = buildRecord(dto, LikeUnlikeTypeEnum.LIKE.getCode());
            affected = noteLikeDOMapper.insertOrUpdate(record);
        } else if (MQConstants.TAG_UNLIKE.equals(tag)) {
            NoteLikeDO record = buildRecord(dto, LikeUnlikeTypeEnum.UNLIKE.getCode());
            affected = noteLikeDOMapper.updateUnlikeNoteByUserIdAndNoteId(record);
        } else {
            throw new IllegalArgumentException("未知的笔记点赞消息标签: " + tag);
        }

        if (affected == 0) {
            return;
        }
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE,
                MessageBuilder.withPayload(body).build(), new SendCallback() {
                    @Override
                    public void onSuccess(SendResult result) {
                        log.info("==> 笔记点赞计数消息发送成功: {}", result);
                    }

                    @Override
                    public void onException(Throwable error) {
                        log.error("==> 笔记点赞计数消息发送失败: ", error);
                    }
                });
    }

    private NoteLikeDO buildRecord(LikeUnlikeNoteMqDTO dto, Integer status) {
        return NoteLikeDO.builder().userId(dto.getUserId()).noteId(dto.getNoteId())
                .createTime(dto.getCreateTime()).status(status.byteValue()).build();
    }
}
