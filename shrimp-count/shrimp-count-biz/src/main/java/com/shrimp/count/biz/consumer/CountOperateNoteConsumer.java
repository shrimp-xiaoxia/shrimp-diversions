package com.shrimp.count.biz.consumer;

import cn.hutool.core.util.ObjUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.constants.RedisKeyConstants;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.enums.NoteOperationEnum;
import com.shrimp.count.biz.model.dto.OperateNoteMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_OPERATE,
        topic = MQConstants.TOPIC_COUNT_NOTE_OPERATE
)
@Slf4j
public class CountOperateNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(Message message) {
        String tags = message.getTags();
        String bodyStr = new String(message.getBody());
        log.info("##[count接受到笔记操作消息]:tag:{},bodyStr:{}", tags, bodyStr);

        if (MQConstants.TAG_NOTE_PUBLISH.equals(tags)) {
            handlePublishTagMessage(bodyStr);
        } else if (MQConstants.TAG_NOTE_DELETE.equals(tags)) {
            handleDeleteTagMessage(bodyStr);
        }
    }

    private void handleDeleteTagMessage(String bodyStr) {
        handleOperateNoteMessage(bodyStr, -1);
    }

    private void handlePublishTagMessage(String bodyStr) {
        handleOperateNoteMessage(bodyStr, 1);
    }

    private void handleOperateNoteMessage(String bodyStr, int count) {
        OperateNoteMqDTO operateNoteMqDTO = JsonUtils.parseJSONString(bodyStr, OperateNoteMqDTO.class);
        if (ObjUtil.isNull(operateNoteMqDTO)) {
            return;
        }
        Long userId = operateNoteMqDTO.getCreatorId();
//        redis
        String countUserKey = RedisKeyConstants.buildCountUserKey(userId);
        Boolean hasKey = redisTemplate.hasKey(countUserKey);
        // 若存在才会更新
        // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.opsForHash().increment(countUserKey, RedisKeyConstants.FIELD_NOTE_TOTAL, count);
        }
//        DB
        userCountDOMapper.insertOrUpdateNoteTotalByUserId(count, userId);
    }

}
