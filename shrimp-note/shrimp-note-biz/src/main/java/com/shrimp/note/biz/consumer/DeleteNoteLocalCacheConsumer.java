package com.shrimp.note.biz.consumer;

import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author ccb
 */
@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "shrimp_group_"+MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,//主题
        messageModel = MessageModel.BROADCASTING //广播模式
)
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {
    @Resource
    private NoteService noteService;

    @Override
    public void onMessage(String message) {
        Long noteId = Long.valueOf(message);
        noteService.deleteNoteDetailLocalCache(noteId);
        log.info("==>成功删除noteId={}的本地缓存",noteId);
    }
}
