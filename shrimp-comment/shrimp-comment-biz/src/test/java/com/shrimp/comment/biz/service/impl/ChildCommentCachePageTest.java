package com.shrimp.comment.biz.service.Impl;

import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.model.vo.FindChildCommentPageListReqVO;
import com.shrimp.comment.biz.rpc.UserRpcService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChildCommentCachePageTest {
    @Test void completeCachedPageDoesNotQueryDatabase() {
        var service = new CommentServiceImpl();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        ZSetOperations<String, Object> sorted = mock(ZSetOperations.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        var mapper = mock(CommentDOMapper.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(redis.opsForValue()).thenReturn(values);
        when(hashes.get(anyString(), any())).thenReturn(8L);
        when(redis.hasKey(anyString())).thenReturn(true);
        when(sorted.rangeByScore(anyString(), eq(0d), eq(Double.POSITIVE_INFINITY), eq(6L), eq(6L)))
                .thenReturn(new java.util.LinkedHashSet<>(List.of("7", "8")));
        when(values.multiGet(anyCollection())).thenReturn(List.of("{\"commentId\":7}", "{\"commentId\":8}"));
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "commentDOMapper", mapper);

        var result = service.findChildCommentPageList(new FindChildCommentPageListReqVO(42L, 2, true));
        assertEquals(List.of(7L, 8L), result.getData().stream().map(item -> item.getCommentId()).toList());
        verifyNoInteractions(mapper);
    }

    @Test void partialCachedPageFallsBackToDatabase() {
        var service = new CommentServiceImpl();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        ZSetOperations<String, Object> sorted = mock(ZSetOperations.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        var mapper = mock(CommentDOMapper.class);
        var users = mock(UserRpcService.class);
        var executor = mock(ThreadPoolTaskExecutor.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(redis.opsForValue()).thenReturn(values);
        when(values.multiGet(anyCollection())).thenReturn(List.of("{\"commentId\":7}"));
        when(hashes.get(anyString(), any())).thenReturn(8L);
        when(redis.hasKey(anyString())).thenReturn(true);
        when(sorted.rangeByScore(anyString(), eq(0d), eq(Double.POSITIVE_INFINITY), eq(6L), eq(6L)))
                .thenReturn(Set.of("7"));
        var first = CommentDO.builder().id(7L).userId(1L).noteId(2L).isContentEmpty(true)
                .createTime(LocalDateTime.now()).build();
        var second = CommentDO.builder().id(8L).userId(1L).noteId(2L).isContentEmpty(true)
                .createTime(LocalDateTime.now()).build();
        when(mapper.selectChildPageList(42L, 6L, 6L)).thenReturn(List.of(first, second));
        when(users.findUserByIds(anyList())).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "commentDOMapper", mapper);
        ReflectionTestUtils.setField(service, "userRpcService", users);
        ReflectionTestUtils.setField(service, "threadPoolTaskExecutor", executor);

        var result = service.findChildCommentPageList(new FindChildCommentPageListReqVO(42L, 2, true));
        assertEquals(List.of(7L, 8L), result.getData().stream().map(item -> item.getCommentId()).toList());
        verify(mapper).selectChildPageList(42L, 6L, 6L);
    }
}
