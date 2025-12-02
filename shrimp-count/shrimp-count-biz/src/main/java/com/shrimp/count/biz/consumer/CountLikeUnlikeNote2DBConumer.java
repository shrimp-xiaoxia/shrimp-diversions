package com.shrimp.count.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB
)
@Slf4j
public class CountLikeUnlikeNote2DBConumer implements RocketMQListener<String> {
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;

    @Override
    public void onMessage(String message) {
//        限制速率
        rateLimiter.acquire();
//        str->mqDTO
        Map<Long, Integer> map;
        try {
            map = JsonUtils.parseMap(message, Long.class, Integer.class);
        } catch (Exception e) {
//            TODO 如何处理格式错误的信息
            log.error("## 解析 JSON 字符串异常", e);
            return;
        }
//        遍历Map
        map.forEach((k, v) -> {
//        构建DO
            NoteCountDO noteCountDO = NoteCountDO.builder()
                    .noteId(k)
                    .likeTotal(Long.valueOf(v.toString()))
                    .build();
//        执行SQL
            noteCountDOMapper.insertOrUpdateLikeCountByUserId(noteCountDO);
        });

    }
}
