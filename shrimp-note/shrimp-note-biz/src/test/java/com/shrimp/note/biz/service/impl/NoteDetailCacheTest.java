package com.shrimp.note.biz.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.FindNoteDetailReqVO;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteDetailCacheTest {
    private final NoteServiceImpl service = new NoteServiceImpl();
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> values = mock(ValueOperations.class);
    private final NoteDOMapper notes = mock(NoteDOMapper.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private Cache<Long, FindNoteDetailRspVO> local;
    private static final Long ID = 90000001234L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "noteDOMapper", notes);
        ReflectionTestUtils.setField(service, "threadPoolTaskExecutor", executor);
        when(redis.opsForValue()).thenReturn(values);
        local = (Cache<Long, FindNoteDetailRspVO>) ReflectionTestUtils.getField(NoteServiceImpl.class, "LOCAL_NOTE_DETAIL_CACHE");
        local.invalidate(ID);
    }

    @AfterEach
    void tearDown() {
        local.invalidate(ID);
        LoginUserContextHolder.removeLoginUserContext();
    }

    @Test
    void privateLocalCacheChecksEveryViewer() {
        local.put(ID, detail(1));
        LoginUserContextHolder.setLoginUserId(7L);
        assertEquals("private", service.findNoteDetail(request()).getData().getTitle());
        LoginUserContextHolder.removeLoginUserContext();
        assertCode(ResponseCodeEnum.NOTE_PRIVATE);
        LoginUserContextHolder.setLoginUserId(8L);
        assertCode(ResponseCodeEnum.NOTE_PRIVATE);
        verifyNoInteractions(redis, notes);
    }

    @Test
    void privateRedisCacheChecksEveryViewer() {
        when(values.get(any())).thenReturn(JsonUtils.toJsonString(detail(1)));
        assertCode(ResponseCodeEnum.NOTE_PRIVATE);
        LoginUserContextHolder.setLoginUserId(8L);
        assertCode(ResponseCodeEnum.NOTE_PRIVATE);
        LoginUserContextHolder.setLoginUserId(7L);
        assertEquals("private", service.findNoteDetail(request()).getData().getTitle());
        verifyNoInteractions(notes);
    }

    @Test
    void publicCachedNoteIsAvailableToGuest() {
        local.put(ID, detail(0));
        assertEquals("private", service.findNoteDetail(request()).getData().getTitle());
    }

    @Test
    void negativeCacheAlwaysReturnsNotFoundWithoutDatabaseQuery() {
        when(values.get(any())).thenReturn("null");
        assertCode(ResponseCodeEnum.NOTE_NOT_EXITS);
        assertCode(ResponseCodeEnum.NOTE_NOT_EXITS);
        verifyNoInteractions(notes, executor);
    }

    @Test
    void cachedDetailWithoutVisibilityFallsBackToDatabase() {
        local.put(ID, FindNoteDetailRspVO.builder().creatorId(7L).build());
        when(values.get(any())).thenReturn("null");
        assertCode(ResponseCodeEnum.NOTE_NOT_EXITS);
        assertNull(local.getIfPresent(ID));
    }

    private void assertCode(ResponseCodeEnum code) {
        BizException failure = assertThrows(BizException.class, () -> service.findNoteDetail(request()));
        assertEquals(code.getErrorCode(), failure.getErrorCode());
    }

    private FindNoteDetailReqVO request() {
        return FindNoteDetailReqVO.builder().id(ID).build();
    }

    private FindNoteDetailRspVO detail(int visible) {
        return FindNoteDetailRspVO.builder().id(ID).creatorId(7L).visible(visible).title("private").build();
    }
}
