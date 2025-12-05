package com.shrimp.count.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.model.dto.AggregationCountCollectUncollectedNoteMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB
)
@Slf4j
public class CountCollectUncollectedNote2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(String message) {
        rateLimiter.acquire();
        log.info("##count接收到收藏数落库消息：{}", message);

//        装换成List
        List<AggregationCountCollectUncollectedNoteMqDTO> list;
        try {
            list = JsonUtils.parseList(message, AggregationCountCollectUncollectedNoteMqDTO.class);
        } catch (Exception e) {
//            TODO 如何处理格式错误的信息
            log.error("## 解析 JSON 字符串异常", e);
            return;
        }
//        遍历list
        for (AggregationCountCollectUncollectedNoteMqDTO agg : list) {
            Long noteId = agg.getNoteId();
            Long noteCreatorId = agg.getNoteCreatorId();
            Integer count = agg.getCount();

            NoteCountDO noteCountDO = NoteCountDO.builder()
                    .noteId(noteId)
                    .collectTotal(Long.valueOf(count))
                    .build();
            //        编程式事务
            transactionTemplate.execute(status -> {
                try {
                    noteCountDOMapper.insertOrUpdateCollectCountByNoteId(noteCountDO);
                    userCountDOMapper.insertOrUpdateCollectsTotalByUserId(count, noteCreatorId);
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
