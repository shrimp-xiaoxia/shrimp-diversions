package com.shrimp.count.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.model.dto.AggregationCountLikeUnlikeNoteMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

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
    @Resource
    private UserCountDOMapper userCountDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void onMessage(String message) {
//        限制速率
        rateLimiter.acquire();
//        str->mqDTO
        List<AggregationCountLikeUnlikeNoteMqDTO> list;
        try {
            list = JsonUtils.parseList(message, AggregationCountLikeUnlikeNoteMqDTO.class);
        } catch (Exception e) {
//            TODO 如何处理格式错误的信息
            log.error("## 解析 JSON 字符串异常", e);
            return;
        }
//        遍历list
        for (AggregationCountLikeUnlikeNoteMqDTO agg : list) {
            Long noteId = agg.getNoteId();
            Long userId = agg.getNoteCreatorId();
            Integer count = agg.getCount();

            NoteCountDO noteCountDO = NoteCountDO.builder()
                    .noteId(noteId)
                    .likeTotal(Long.valueOf(count))
                    .build();
            transactionTemplate.execute(status -> {
                try {
                    noteCountDOMapper.insertOrUpdateLikeCountByNoteId(noteCountDO);
                    userCountDOMapper.insertOrUpdateLikesTotalByUserId(count, userId);
                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    log.error("", e);
                }
                return false;
            });
        }

    }
}
