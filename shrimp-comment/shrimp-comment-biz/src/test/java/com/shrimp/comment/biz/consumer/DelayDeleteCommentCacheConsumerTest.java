package com.shrimp.comment.biz.consumer;

import com.shrimp.comment.biz.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class DelayDeleteCommentCacheConsumerTest {
    @Test
    void childDeletionInvalidatesParentDetailAndLocalCache() {
        var consumer = new DelayDeleteCommentCacheConsumer();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ZSetOperations<String, Object> zset = mock(ZSetOperations.class);
        var comments = mock(CommentService.class);
        when(redis.opsForZSet()).thenReturn(zset);
        ReflectionTestUtils.setField(consumer, "redisTemplate", redis);
        ReflectionTestUtils.setField(consumer, "commentService", comments);

        consumer.onMessage("{\"commentId\":10,\"level\":2,\"noteId\":40,\"parentId\":20}");

        verify(redis).delete("comment:detail:20");
        verify(comments).removeCommentLocalCache(20L);
    }
}
