package com.shrimp.user.biz.service;

import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.service.Impl.UserServiceImpl;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class UserBatchLookupTest {
    @Test void combinesPartialCacheHitWithDatabaseResults() {
        var service = new UserServiceImpl();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        var mapper = mock(UserDOMapper.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.multiGet(anyCollection())).thenReturn(Arrays.asList("{\"id\":1,\"nickName\":\"cached\"}", null));
        when(mapper.selectByUserIds(List.of(2L))).thenReturn(List.of(UserDO.builder().id(2L).nickname("database").build()));
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "userDOMapper", mapper);
        ReflectionTestUtils.setField(service, "threadPoolTaskExecutor", mock(ThreadPoolTaskExecutor.class));
        var result = service.findUserByIds(new FindUserByIdsReqDTO(List.of(1L, 2L)));
        verify(values).multiGet(List.of("user:info:cancel:1", "user:info:cancel:2"));
        assertTrue(result.isSuccess());
        assertEquals(Set.of(1L, 2L), result.getData().stream().map(user -> user.getId()).collect(Collectors.toSet()));
    }
}
