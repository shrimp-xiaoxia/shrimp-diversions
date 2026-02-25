package com.shrimp.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.biz.model.dto.CountPublishCommentMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT
)
@Slf4j
public class CountNoteCommentConsumer implements RocketMQListener<String> {
    @Resource
    private NoteCountDOMapper noteCountDOMapper;

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
        log.info("==> 【笔记评论数】聚合消息, size: {}", messages.size());
        log.info("==> 【笔记评论数】聚合消息, {}", JsonUtils.toJsonString(messages));
//        转换成dto
        List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();
        messages.forEach(message -> {
            try {
                List<CountPublishCommentMqDTO> countPublishCommentMqDTOS = JsonUtils.parseList(message, CountPublishCommentMqDTO.class);
                countPublishCommentMqDTOList.addAll(countPublishCommentMqDTOS);
            } catch (Exception e) {
                log.error("",e);
            }
        });

        if (CollUtil.isEmpty(countPublishCommentMqDTOList)) {
            return;
        }

//        同一个NoteID下合并
        Map<Long, List<CountPublishCommentMqDTO>> mapByNoteId = countPublishCommentMqDTOList.stream()
                .collect(Collectors.groupingBy(CountPublishCommentMqDTO::getNoteId));
        mapByNoteId.entrySet().stream().forEach(entry -> {
            Long noteId = entry.getKey();
            int count = entry.getValue().size();
//        插入数据
            if (count > 0) {
                noteCountDOMapper.insertOrUpdateCommentCountByNoteId(noteId, count);
            }
        });
    }
}
