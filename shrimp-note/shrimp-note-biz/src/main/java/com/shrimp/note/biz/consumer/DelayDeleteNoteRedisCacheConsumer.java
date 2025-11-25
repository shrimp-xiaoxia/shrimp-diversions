package com.shrimp.note.biz.consumer;

import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.constants.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
                        topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE
)
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;


    @Override
    public void onMessage(String message) {
        Long noteId = Long.valueOf(message);
        String noteDetailCacheKey = RedisKeyConstants.buildNoteDetailCacheKey(noteId);
        redisTemplate.delete(noteDetailCacheKey);
        log.info("## 延迟删除Redis缓存消息消费成功, noteId: {}", noteId);
//      再次删除其他节点的本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("## 延迟删除本地缓存消息消费成功, noteId: {}", noteId);
    }
}
