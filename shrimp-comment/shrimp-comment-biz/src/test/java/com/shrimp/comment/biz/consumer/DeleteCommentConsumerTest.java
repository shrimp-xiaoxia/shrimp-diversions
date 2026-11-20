package com.shrimp.comment.biz.consumer;

import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.framework.common.util.JsonUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeleteCommentConsumerTest {
    private DeleteCommentConsumer consumer;
    private CommentDOMapper mapper;
    private RocketMQTemplate mq;
    private NoteCountDOMapper noteCounts;
    private RedisTemplate<String, Object> redis;

    @BeforeEach
    void setUp() {
        consumer = new DeleteCommentConsumer();
        mapper = mock(CommentDOMapper.class);
        mq = mock(RocketMQTemplate.class);
        redis = mock(RedisTemplate.class);
        noteCounts = mock(NoteCountDOMapper.class);
        when(redis.hasKey(any())).thenReturn(false);
        ReflectionTestUtils.setField(consumer, "commentDOMapper", mapper);
        ReflectionTestUtils.setField(consumer, "redisTemplate", redis);
        ReflectionTestUtils.setField(consumer, "rocketMQTemplate", mq);
        ReflectionTestUtils.setField(consumer, "noteCountDOMapper", noteCounts);
    }

    private String deletedReply() {
        return JsonUtils.toJsonString(CommentDO.builder().id(10L).parentId(20L).noteId(40L).level(2).build());
    }

    @Test
    void emptyDescendantsSkipDeleteButSendParentHeatUpdate() {
        when(mapper.selectReplyIdByBeRepliedId(10L)).thenReturn(List.of());
        when(mapper.selectByPrimaryKey(20L)).thenReturn(CommentDO.builder().id(20L).firstReplyCommentId(30L).build());

        consumer.onMessage(deletedReply());

        verify(mapper, never()).batchDeleteCommentByIds(anyList());
        verify(mapper).updateChildCommentTotalById(20L, -1L);
        verify(noteCounts).updateCommentTotalByNoteId(40L, -1L);
        verify(redis).delete("count:comment:20");
        verify(redis).delete("count:note:40");
        verify(mq).asyncSend(eq(MQConstants.TOPIC_COMMENT_HEAT_UPDATE), argThat((Message<?> message) -> {
            Object payload = message.getPayload();
            return "[20]".equals(payload.toString());
        }), any(SendCallback.class));
    }

    @Test
    void missingParentSkipsFirstReplyAndHeatUpdate() {
        when(mapper.selectReplyIdByBeRepliedId(10L)).thenReturn(List.of());
        when(mapper.selectByPrimaryKey(20L)).thenReturn(null);

        consumer.onMessage(deletedReply());

        verify(mapper, never()).batchDeleteCommentByIds(anyList());
        verify(mapper, never()).batchSelectEarliestByParentIds(anyList());
        verifyNoInteractions(mq);
    }

    @Test
    void nonemptyDescendantsAreDeleted() {
        when(mapper.selectReplyIdByBeRepliedId(10L)).thenReturn(List.of(11L));
        when(mapper.selectReplyIdByBeRepliedId(11L)).thenReturn(List.of());
        when(mapper.batchDeleteCommentByIds(List.of(11L))).thenReturn(1L);
        when(mapper.selectByPrimaryKey(20L)).thenReturn(CommentDO.builder().id(20L).firstReplyCommentId(30L).build());

        consumer.onMessage(deletedReply());

        verify(mapper).batchDeleteCommentByIds(List.of(11L));
        verify(mapper).updateChildCommentTotalById(20L, -2L);
        verify(noteCounts).updateCommentTotalByNoteId(40L, -2L);
        assertEquals(2, mockingDetails(mq).getInvocations().size());
    }

    @Test
    void deletingFirstReplyClearsPointerWhenNoRepliesRemain() {
        when(mapper.selectReplyIdByBeRepliedId(10L)).thenReturn(List.of());
        when(mapper.selectByPrimaryKey(20L)).thenReturn(CommentDO.builder().id(20L).firstReplyCommentId(10L).build());
        when(mapper.batchSelectEarliestByParentIds(List.of(20L))).thenReturn(List.of());
        consumer.onMessage(deletedReply());
        verify(mapper).updateFirstReplyAfterDelete(20L, 0L);
    }

    @Test
    void mapperSafelyHandlesEmptyIds() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/CommentDOMapper.xml";
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement("com.shrimp.comment.biz.domain.mapper.CommentDOMapper.batchDeleteCommentByIds");
        String emptySql = statement.getBoundSql(Map.of("commentIds", List.of())).getSql().replaceAll("\\s+", " ").trim();
        String populatedSql = statement.getBoundSql(Map.of("commentIds", List.of(11L))).getSql().replaceAll("\\s+", " ").trim();
        assertTrue(emptySql.contains("where 1 = 0"));
        assertTrue(populatedSql.contains("where id in"));
    }
}
