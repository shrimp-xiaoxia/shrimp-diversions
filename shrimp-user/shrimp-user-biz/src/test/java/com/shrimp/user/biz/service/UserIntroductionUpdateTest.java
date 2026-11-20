package com.shrimp.user.biz.service;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.model.vo.UpdateUserInfoReqVO;
import com.shrimp.user.biz.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserIntroductionUpdateTest {
    @AfterEach void clearContext() { LoginUserContextHolder.removeLoginUserContext(); }

    @Test void emptyIntroductionClearsExistingValue() {
        var service = new UserServiceImpl();
        var mapper = mock(UserDOMapper.class);
        var redis = mock(RedisTemplate.class);
        ReflectionTestUtils.setField(service, "userDOMapper", mapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        when(mapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        LoginUserContextHolder.setLoginUserId(7001L);
        var request = new UpdateUserInfoReqVO();
        request.setIntroduction("");

        service.updateUserInfo(request);

        var updated = ArgumentCaptor.forClass(UserDO.class);
        verify(mapper).updateByPrimaryKeySelective(updated.capture());
        assertEquals("", updated.getValue().getIntroduction());
        assertEquals(7001L, updated.getValue().getId());
        verify(redis).delete("user:info:cancel:7001");
    }

    @Test void omittedIntroductionDoesNotUpdateUser() {
        var service = new UserServiceImpl();
        var mapper = mock(UserDOMapper.class);
        ReflectionTestUtils.setField(service, "userDOMapper", mapper);
        LoginUserContextHolder.setLoginUserId(7001L);

        service.updateUserInfo(new UpdateUserInfoReqVO());

        verifyNoInteractions(mapper);
    }
}
