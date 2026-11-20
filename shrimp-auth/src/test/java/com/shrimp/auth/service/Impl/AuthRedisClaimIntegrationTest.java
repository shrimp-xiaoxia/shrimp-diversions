package com.shrimp.auth.service.Impl;

import com.shrimp.auth.config.RedisTemplateConfig;
import com.shrimp.auth.constants.RedisKeyConstants;
import com.shrimp.auth.model.vo.user.UserLoginReqVO;
import com.shrimp.auth.rpc.UserRpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "SHRIMP_REDIS_CLAIM_TEST", matches = "true")
class AuthRedisClaimIntegrationTest {
    @Test
    void releaseOnlyDeletesMatchingClaimWithConfiguredSerializers() {
        var config = new RedisStandaloneConfiguration(
                System.getenv("SHRIMP_TEST_REDIS_HOST"),
                Integer.parseInt(System.getenv("SHRIMP_TEST_REDIS_PORT")));
        config.setDatabase(Integer.parseInt(System.getenv("SHRIMP_TEST_REDIS_DB")));
        String username = System.getenv("SHRIMP_TEST_REDIS_USERNAME");
        if (username != null && !username.isBlank()) config.setUsername(username);
        String password = System.getenv("SHRIMP_TEST_REDIS_PASSWORD");
        if (password != null && !password.isBlank()) config.setPassword(RedisPassword.of(password));
        var factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        var redis = new RedisTemplateConfig().redisTemplate(factory);
        var service = new AuthServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        String key = "codex:shrimp-frontend:claim-test:" + UUID.randomUUID();
        String owner = UUID.randomUUID().toString();
        boolean created = false;
        try {
            created = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, 60, TimeUnit.SECONDS));
            assertTrue(created, "独立测试键创建失败");
            ReflectionTestUtils.invokeMethod(service, "releaseClaim", key, UUID.randomUUID().toString());
            assertEquals(owner, redis.opsForValue().get(key));
            ReflectionTestUtils.invokeMethod(service, "releaseClaim", key, owner);
            assertFalse(Boolean.TRUE.equals(redis.hasKey(key)));
        } finally {
            if (created) redis.delete(key);
            factory.destroy();
        }
    }

    @Test
    void registrationExceptionReleasesClaimButKeepsVerificationKey() {
        var config = new RedisStandaloneConfiguration(
                System.getenv("SHRIMP_TEST_REDIS_HOST"),
                Integer.parseInt(System.getenv("SHRIMP_TEST_REDIS_PORT")));
        config.setDatabase(Integer.parseInt(System.getenv("SHRIMP_TEST_REDIS_DB")));
        String username = System.getenv("SHRIMP_TEST_REDIS_USERNAME");
        if (username != null && !username.isBlank()) config.setUsername(username);
        String password = System.getenv("SHRIMP_TEST_REDIS_PASSWORD");
        if (password != null && !password.isBlank()) config.setPassword(RedisPassword.of(password));
        var factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        var redis = new RedisTemplateConfig().redisTemplate(factory);
        var service = new AuthServiceImpl();
        var users = mock(UserRpcService.class);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "userRpcService", users);
        String phone = "codex-test-" + UUID.randomUUID(); // 非真实手机号，直接调用服务方法。
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        String usedKey = key + ":used";
        String code = "synthetic-test-code";
        boolean created = false;
        try {
            created = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, code, 60, TimeUnit.SECONDS));
            assertTrue(created, "独立测试键创建失败");
            when(users.register(phone)).thenThrow(new IllegalStateException("模拟注册 RPC 失败"));
            var request = UserLoginReqVO.builder().phone(phone).type(1).code(code).build();
            assertThrows(IllegalStateException.class, () -> service.loginAndRegister(request));
            assertEquals(code, redis.opsForValue().get(key));
            assertFalse(Boolean.TRUE.equals(redis.hasKey(usedKey)));
            verify(users).register(phone);

            String newOwner = UUID.randomUUID().toString();
            doAnswer(invocation -> {
                // 模拟旧请求结束前，标记到期并被另一请求持有。
                redis.opsForValue().set(usedKey, newOwner, 60, TimeUnit.SECONDS);
                throw new IllegalStateException("模拟旧请求 RPC 失败");
            }).when(users).register(phone);
            assertThrows(IllegalStateException.class, () -> service.loginAndRegister(request));
            assertEquals(newOwner, redis.opsForValue().get(usedKey));
        } finally {
            if (created) {
                redis.delete(usedKey);
                redis.delete(key);
            }
            factory.destroy();
        }
    }
}
