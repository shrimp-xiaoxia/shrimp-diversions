package com.shrimp.user.relation.biz.service;

import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowStateQueryServiceTest {
    private final FollowingDOMapper mapper = mock(FollowingDOMapper.class);
    private final FollowStateQueryService service = new FollowStateQueryService(mapper);

    @Test void returnsPersistedRelationship() {
        when(mapper.selectFollowingIds(1L, List.of(2L))).thenReturn(List.of(2L));
        var result = service.find(1L, 2L);
        assertTrue(result.available());
        assertEquals(Boolean.TRUE, result.followed());
    }

    @Test void distinguishesAbsentFromUnavailable() {
        when(mapper.selectFollowingIds(1L, List.of(2L))).thenReturn(List.of());
        var result = service.find(1L, 2L);
        assertTrue(result.available());
        assertEquals(Boolean.FALSE, result.followed());
        when(mapper.selectFollowingIds(1L, List.of(2L))).thenThrow(new IllegalStateException());
        var failed = service.find(1L, 2L);
        assertFalse(failed.available());
        assertNull(failed.followed());
    }

    @Test void doesNotQueryWithoutViewer() {
        var result = service.find(null, 2L);
        assertFalse(result.available());
        assertNull(result.followed());
        verifyNoInteractions(mapper);
    }
}
