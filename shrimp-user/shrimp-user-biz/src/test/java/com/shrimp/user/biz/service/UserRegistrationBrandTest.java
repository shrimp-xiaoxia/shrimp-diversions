package com.shrimp.user.biz.service;

import com.shrimp.user.biz.domain.dataobject.RoleDO;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.RoleDOMapper;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.domain.mapper.UserRoleDOMapper;
import com.shrimp.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.shrimp.user.biz.service.Impl.UserServiceImpl;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRegistrationBrandTest {
    @Test void newUserReceivesShrimpCommunityNickname() {
        var service = new UserServiceImpl();
        var users = mock(UserDOMapper.class);
        var roles = mock(RoleDOMapper.class);
        var userRoles = mock(UserRoleDOMapper.class);
        var ids = mock(DistributedIdGeneratorRpcService.class);
        @SuppressWarnings("unchecked") RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, Object> values = mock(ValueOperations.class);
        ReflectionTestUtils.setField(service, "userDOMapper", users);
        ReflectionTestUtils.setField(service, "roleDOMapper", roles);
        ReflectionTestUtils.setField(service, "userRoleDOMapper", userRoles);
        ReflectionTestUtils.setField(service, "distributedIdGeneratorRpcService", ids);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        when(ids.getXiaohashuId()).thenReturn("123456");
        when(ids.getUserId()).thenReturn("7001");
        when(roles.selectByPrimaryKey(any())).thenReturn(RoleDO.builder().roleKey("user").build());
        when(redis.opsForValue()).thenReturn(values);

        service.registerUser(RegisterUserReqDTO.builder().phone("13800000000").build());

        var captured = ArgumentCaptor.forClass(UserDO.class);
        verify(users).insert(captured.capture());
        assertEquals("虾趣用户123456", captured.getValue().getNickname());
    }
}
