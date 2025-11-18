package com.shrimp.gateway.constants;

public class RedisKeyConstants {
    private static final String USER_ROLE_KEY_PREFIX = "user:roles:";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
    public static final String SA_TOKEN_TOKEN_KEY_PREFIX = "Authorization:login:token:";
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLE_KEY_PREFIX + userId;
    }

    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }
}
