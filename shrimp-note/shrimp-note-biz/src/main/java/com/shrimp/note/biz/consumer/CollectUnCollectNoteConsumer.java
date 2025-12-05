package com.shrimp.note.biz.consumer;

import cn.hutool.core.util.ObjUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.domain.dataobject.NoteCollectionDO;
import com.shrimp.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.shrimp.note.biz.enums.CollectUnCollectTypeEnum;
import com.shrimp.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COLLECT_OR_UNCOLLECTED,
        topic = MQConstants.TOPIC_COLLECT_OR_UNCOLLECTED
)
@Slf4j
public class CollectUnCollectNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();
        String tag = message.getTags();
        byte[] body = message.getBody();
        String bodyJSONStr = new String(body);
        log.info("==>收藏消费端成功接收到消息,tag:{},body:{}", tag, bodyJSONStr);

        if (MQConstants.TAG_COLLECT.equals(tag)) {
            handleCollectTagMessage(bodyJSONStr);
        } else if (MQConstants.TAG_UNCOLLECTED.equals(tag)) {
            handleUnCollectTagMessage(bodyJSONStr);
        }
    }

    private void handleUnCollectTagMessage(String bodyJSONStr) {
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = JsonUtils.parseJSONString(bodyJSONStr, CollectUnCollectNoteMqDTO.class);
        if (ObjUtil.isNull(collectUnCollectNoteMqDTO)) {
            return;
        }

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(collectUnCollectNoteMqDTO.getUserId())
                .noteId(collectUnCollectNoteMqDTO.getNoteId())
                .status(Byte.valueOf(CollectUnCollectTypeEnum.UNCOLLECTED.getCode().toString()))
                .createTime(collectUnCollectNoteMqDTO.getCreateTime())
                .build();

        int count = noteCollectionDOMapper.updateUnCollect(noteCollectionDO);

        if (count == 0) {
            return;
        }

        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJSONStr).build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记取消收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记取消收藏】MQ 发送异常: ", throwable);
            }
        });
    }

    private void handleCollectTagMessage(String bodyJSONStr) {
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = JsonUtils.parseJSONString(bodyJSONStr, CollectUnCollectNoteMqDTO.class);
        if (ObjUtil.isNull(collectUnCollectNoteMqDTO)) {
            return;
        }

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(collectUnCollectNoteMqDTO.getUserId())
                .noteId(collectUnCollectNoteMqDTO.getNoteId())
                .status(Byte.valueOf(CollectUnCollectTypeEnum.COLLECT.getCode().toString()))
                .createTime(collectUnCollectNoteMqDTO.getCreateTime())
                .build();

        int count = noteCollectionDOMapper.insertOrUpdate(noteCollectionDO);

        if (count == 0) {
            return;
        }

        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJSONStr).build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记收藏】MQ 发送异常: ", throwable);
            }
        });
    }
}
