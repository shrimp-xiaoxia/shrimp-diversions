package com.shrimp.note.biz.consumer;

import cn.hutool.core.util.ObjUtil;
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

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
        consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    // 每秒创建 5000 个令牌 TODO 该方法无法动态变化
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();
        if (ObjUtil.isNull(message)) {
            return;
        }
        String tags = message.getTags();
        String bodyStr = new String(message.getBody());

        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyStr, tags);

        if (MQConstants.TAG_LIKE.equals(tags)) {
            handleLikeNoteTagMessage(bodyStr);
        } else if (MQConstants.TAG_UNLIKE.equals(tags)) {
            handleUnlikeNoteTagMessage(bodyStr);
        }
    }

    private void handleUnlikeNoteTagMessage(String bodyStr) {
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseJSONString(bodyStr, LikeUnlikeNoteMqDTO.class);
        if (ObjUtil.isNull(likeUnlikeNoteMqDTO)) {
            return;
        }
//        数据库落库
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(likeUnlikeNoteMqDTO.getUserId())
                .noteId(likeUnlikeNoteMqDTO.getNoteId())
                .status(Byte.valueOf(LikeUnlikeTypeEnum.UNLIKE.getCode().toString()))
                .createTime(likeUnlikeNoteMqDTO.getCreateTime())
                .build();
        int count = noteLikeDOMapper.updateUnlikeNoteByUserIdAndNoteId(noteLikeDO);
        if (count == 0) return;
//       发送MQ到count服务
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyStr).build();
        String topicCountNoteLike = MQConstants.TOPIC_COUNT_NOTE_LIKE;

        rocketMQTemplate.asyncSend(topicCountNoteLike, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });

    }

    private void handleLikeNoteTagMessage(String bodyStr) {
//        装换数据
        LikeUnlikeNoteMqDTO likeNoteMqDTO = JsonUtils.parseJSONString(bodyStr, LikeUnlikeNoteMqDTO.class);
        if (ObjUtil.isNull(likeNoteMqDTO)) {
            return;
        }
//        数据库落库
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(likeNoteMqDTO.getUserId())
                .noteId(likeNoteMqDTO.getNoteId())
                .createTime(likeNoteMqDTO.getCreateTime())
                .status(Byte.valueOf(LikeUnlikeTypeEnum.LIKE.getCode().toString()))
                .build();
        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);
        if (count == 0) return;
//         发送MQ到count
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyStr).build();
        String topicCountNoteLike = MQConstants.TOPIC_COUNT_NOTE_LIKE;

        rocketMQTemplate.asyncSend(topicCountNoteLike, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记点赞】MQ 发送异常: ", throwable);
            }
        });

    }
}
