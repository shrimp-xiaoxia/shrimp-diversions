package com.shrimp.user.biz.runner;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.shrimp.user.biz.constants.RedisKeyConstants;
import com.shrimp.user.biz.domain.dataobject.PermissionDO;
import com.shrimp.user.biz.domain.dataobject.RoleDO;
import com.shrimp.user.biz.domain.dataobject.RolePermissionDO;
import com.shrimp.user.biz.domain.mapper.PermissionDOMapper;
import com.shrimp.user.biz.domain.mapper.RoleDOMapper;
import com.shrimp.user.biz.domain.mapper.RolePermissionDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PushRolePermissions2RedisRunner implements ApplicationRunner {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private RolePermissionDOMapper rolePermissionDOMapper;
    @Resource
    private PermissionDOMapper permissionDOMapper;

    @Override
    public void run(ApplicationArguments args) {
//      使用分布式锁避免在一定时间内重复执行该方法
//        原子操作，只有在键 PUSH_PERMISSION_FLAG 不存在时，才会设置该键的值为 "1"，并设置过期时间为 1 天
        try {
            Boolean lock = redisTemplate.opsForValue().setIfAbsent(RedisKeyConstants.PUSH_PERMISSION_FLAG, "one day", 1, TimeUnit.DAYS);
            if (Boolean.FALSE.equals(lock)) {
                log.warn("==> 角色权限数据已经同步至 Redis 中，不再同步...");
                return;
            }
            log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");
//        查询启用的全部角色
            List<RoleDO> roleDOS = roleDOMapper.selectEnabledRoles();
            if (CollectionUtils.isEmpty(roleDOS)) {
                return;
            }
//        查询这些角色对应的全部权限
            List<Long> roleIds = roleDOS.stream().map(RoleDO::getId).toList();
            List<RolePermissionDO> rolePermissionDOS = rolePermissionDOMapper.selectByRoleIds(roleIds);
//        根据角色ID组成map
            Map<Long, List<Long>> rolePermissionMap = rolePermissionDOS.stream()
                    .collect(Collectors.groupingBy(RolePermissionDO::getRoleId, Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList())));
//        查询启用的全部权限
            List<PermissionDO> permissionDOS = permissionDOMapper.selectAppEnabledList();
//        根据权限ID来组成map
            Map<Long, PermissionDO> permissionDOMap = permissionDOS
                    .stream().collect(Collectors.toMap(PermissionDO::getId, permissionDO -> permissionDO));

            //        遍历角色ID对应的权限，根据查询出来的可用权限，加入到最终的map集合中
            HashMap<String, List<String>> rolePermissionToRedisMap = Maps.newHashMap();
            roleDOS.forEach(roleDO -> {
                Long roleId = roleDO.getId();
                List<Long> permissionIds = rolePermissionMap.get(roleId);
                if (CollUtil.isNotEmpty(permissionIds)) {
                    ArrayList<String> enabledPermissionKeys = Lists.newArrayList();
                    permissionIds.forEach(permissionId -> {
                        if (Objects.nonNull(permissionDOMap.get(permissionId))) {
                            enabledPermissionKeys.add(permissionDOMap.get(permissionId).getPermissionKey());
                        }
                    });
                    rolePermissionToRedisMap.put(roleDO.getRoleKey(), enabledPermissionKeys);
                }
            });
//        存储到Redis中
//          同步至 Redis 中，方便后续网关查询鉴权使用
            rolePermissionToRedisMap.forEach((roleKey, permissionKeys) -> {
                redisTemplate.opsForValue().set(RedisKeyConstants.buildRolePermissionsKey(roleKey), JSONUtil.toJsonStr(permissionKeys));
            });
        } catch (Exception e) {
//        有异常则释放锁
            redisTemplate.delete(RedisKeyConstants.PUSH_PERMISSION_FLAG);
            log.error("==> 同步角色权限数据到 Redis 中失败: ", e);
        }
        log.info("==> 服务成功同步角色权限数据到 Redis 中...");
    }
}
