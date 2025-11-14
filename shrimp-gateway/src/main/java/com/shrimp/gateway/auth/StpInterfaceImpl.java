package com.shrimp.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrimp.gateway.constants.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
//        TODO 从Redis中返回此 loginId 拥有的权限列表
        log.info("开始获取用户的对应权限");
        List<String> roleKeyList = getRoleList(loginId, loginType);
        if (CollUtil.isEmpty(roleKeyList)) {
            return Collections.emptyList();
        }
//        使用Redis聚合查询key
        List<String> rolePermissionKeys = roleKeyList.stream().map(RedisKeyConstants::buildRolePermissionsKey).toList();
        List<String> PermissionKeysList = redisTemplate.opsForValue().multiGet(rolePermissionKeys);

        HashSet<String> resultMap = new HashSet<>();
        if (CollUtil.isNotEmpty(PermissionKeysList)) {
            PermissionKeysList.forEach(permissionKeysStr -> {
                if (StrUtil.isNotBlank(permissionKeysStr)) {
                    try {
                        List<String> permissionList = objectMapper.readValue(permissionKeysStr, new TypeReference<>() {
                        });
                        resultMap.addAll(permissionList);
                    } catch (JsonProcessingException e) {
                        log.error("==> JSON 解析错误: ", e);
                    }
                }
            });
        }

        return resultMap.stream().toList();
    }

    @SneakyThrows
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        log.info("开始获取该用户的角色");
//        TODO 从Redis中返回此  loginId拥有的角色列表
//        获取对应的redisKey
        String userRoleKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
//        从redis中获取到数据  
        String roleListStr = redisTemplate.opsForValue().get(userRoleKey);
        if (StringUtils.isBlank(roleListStr)) {
            return Collections.emptyList();
        }
//        如何转化成List<String>，使用Jackson中的核心类ObjectMapper
        return objectMapper.readValue(roleListStr, new TypeReference<>() {
        });
    }
}
