package com.shrimp.user.biz.constants;

public class RedisKeyConstants {
    public static final String SHRIMP_ID_GENERATOR_KEY = "shrimp_id_generator";
    private static final String USER_ROLE_KEY_PREFIX = "user:roles:";
    // 权限同步标记 Key
    public static final String PUSH_PERMISSION_FLAG = "push.permission.flag";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
//    用户信息缓存key前缀
    private static final String USER_INFO_CANCEL_KEY_PREFIX = "user:info:cancel:";


    public static String buildUserRoleKey(Long userId) {
        return USER_ROLE_KEY_PREFIX + userId;
    }

    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }

    public static String buildUserInfoCancelKey(Long userId) {
        return USER_INFO_CANCEL_KEY_PREFIX + userId;
    }
}
