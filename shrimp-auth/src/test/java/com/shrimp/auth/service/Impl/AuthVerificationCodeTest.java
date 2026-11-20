package com.shrimp.auth.service.Impl;
import com.shrimp.auth.model.vo.user.UserLoginReqVO;
import com.shrimp.auth.rpc.UserRpcService;
import com.shrimp.auth.constants.RedisKeyConstants;
import com.shrimp.framework.common.response.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.concurrent.TimeUnit;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthVerificationCodeTest {
 @Test void usedCodeMustNotRegisterOrLoginAgain() {
  RedisTemplate<String,Object> redis=mock(RedisTemplate.class);
  ValueOperations<String,Object> values=mock(ValueOperations.class);
  when(redis.opsForValue()).thenReturn(values);
  when(values.get(anyString())).thenReturn("123456");
  when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(120000L);
  when(values.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);
  UserRpcService users=mock(UserRpcService.class);
  AuthServiceImpl auth=new AuthServiceImpl();
  ReflectionTestUtils.setField(auth,"redisTemplate",redis);
  ReflectionTestUtils.setField(auth,"userRpcService",users);
  Response<String> result=auth.loginAndRegister(UserLoginReqVO.builder().phone("19844121131").type(1).code("123456").build());
  assertFalse(result.isSuccess());
  assertEquals("AUTH-20101",result.getErrorCode());
  verifyNoInteractions(users);
 }
 @Test void releasesClaimWhenRegistrationReturnsNoId() {
  RedisTemplate<String,Object> redis=mock(RedisTemplate.class);
  ValueOperations<String,Object> values=mock(ValueOperations.class);
  when(redis.opsForValue()).thenReturn(values);
  when(values.get(anyString())).thenReturn("123456");
  when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(120000L);
  when(values.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
  UserRpcService users=mock(UserRpcService.class);
  when(users.register(anyString())).thenReturn(null);
  AuthServiceImpl auth=new AuthServiceImpl();
  ReflectionTestUtils.setField(auth,"redisTemplate",redis);
  ReflectionTestUtils.setField(auth,"userRpcService",users);
  assertThrows(RuntimeException.class, () -> auth.loginAndRegister(UserLoginReqVO.builder().phone("13800000000").type(1).code("123456").build()));
  verify(redis).execute(any(RedisScript.class), anyList(), any());
 }
 @Test void releasesClaimWhenRegistrationThrowsBeforeLogin() {
  RedisTemplate<String,Object> redis=mock(RedisTemplate.class);
  ValueOperations<String,Object> values=mock(ValueOperations.class);
  when(redis.opsForValue()).thenReturn(values);
  when(values.get(anyString())).thenReturn("123456");
  when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(120000L);
  when(values.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
  UserRpcService users=mock(UserRpcService.class);
  when(users.register(anyString())).thenThrow(new IllegalStateException("模拟调用结果未知"));
  AuthServiceImpl auth=new AuthServiceImpl();
  ReflectionTestUtils.setField(auth,"redisTemplate",redis);
  ReflectionTestUtils.setField(auth,"userRpcService",users);
  assertThrows(IllegalStateException.class, () -> auth.loginAndRegister(UserLoginReqVO.builder().phone("13800000000").type(1).code("123456").build()));
  var claim = ArgumentCaptor.forClass(Object.class);
  verify(values).setIfAbsent(anyString(), claim.capture(), eq(120000L), eq(TimeUnit.MILLISECONDS));
  verify(redis).execute(any(RedisScript.class), eq(java.util.List.of(RedisKeyConstants.buildVerificationCodeKey("13800000000") + ":used")), eq(claim.getValue()));
 }
 @Test void preservesRegistrationFailureWhenClaimReleaseAlsoFails() {
  RedisTemplate<String,Object> redis=mock(RedisTemplate.class);
  ValueOperations<String,Object> values=mock(ValueOperations.class);
  when(redis.opsForValue()).thenReturn(values);
  when(values.get(anyString())).thenReturn("123456");
  when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(120000L);
  when(values.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
  UserRpcService users=mock(UserRpcService.class);
  var registrationFailure=new IllegalStateException("模拟注册服务失败");
  var releaseFailure=new IllegalStateException("模拟标记释放失败");
  when(users.register(anyString())).thenThrow(registrationFailure);
  when(redis.execute(any(RedisScript.class), anyList(), any())).thenThrow(releaseFailure);
  AuthServiceImpl auth=new AuthServiceImpl();
  ReflectionTestUtils.setField(auth,"redisTemplate",redis);
  ReflectionTestUtils.setField(auth,"userRpcService",users);
  var thrown=assertThrows(IllegalStateException.class, () -> auth.loginAndRegister(UserLoginReqVO.builder().phone("13800000000").type(1).code("123456").build()));
  assertSame(registrationFailure,thrown);
  assertArrayEquals(new Throwable[]{releaseFailure},thrown.getSuppressed());
 }
}
