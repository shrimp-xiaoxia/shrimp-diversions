package com.shrimp.comment.biz.service.Impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.shrimp.comment.biz.model.vo.FindCommentPageListReqVO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentMixedCacheTest {
    @Test void combinesLocalAndRedisCommentHits() {
        var service = new CommentServiceImpl();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        ZSetOperations<String, Object> sorted = mock(ZSetOperations.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(redis.opsForValue()).thenReturn(values);
        when(hashes.get(anyString(), any())).thenReturn(2L);
        when(redis.hasKey(anyString())).thenReturn(true);
        when(sorted.reverseRangeByScore(anyString(), anyDouble(), anyDouble(), eq(0L), eq(10L)))
                .thenReturn(new LinkedHashSet<>(List.of("981001", "981002")));
        when(values.multiGet(anyCollection())).thenReturn(List.of("{\"commentId\":981002,\"heat\":1}"));
        when(redis.executePipelined(any(SessionCallback.class))).thenReturn(List.of(Map.of("x", "x"), Map.of("x", "x")));
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "commentDOMapper", mock(CommentDOMapper.class));
        ReflectionTestUtils.setField(service, "threadPoolTaskExecutor", mock(ThreadPoolTaskExecutor.class));
        Cache<Long, String> cache = (Cache<Long, String>) ReflectionTestUtils.getField(CommentServiceImpl.class, "LOCAL_CACHE");
        cache.invalidateAll(List.of(981001L, 981002L));
        cache.put(981001L, "{\"commentId\":981001,\"heat\":2}");
        cache.put(981002L, "");
        try {
            var request = new FindCommentPageListReqVO(77L, 1);
            var result = service.findCommentPageList(request);
            assertEquals(Set.of(981001L, 981002L), new HashSet<>(result.getData().stream().map(c -> c.getCommentId()).toList()));
        } finally {
            cache.invalidateAll(List.of(981001L, 981002L));
        }
    }
}
