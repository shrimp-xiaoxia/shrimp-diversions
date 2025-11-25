package com.shrimp.user.relation.biz.constants;

public class RedisKeyConstants {
    private final static String FOLLOW_USER_ZSET_KEY_PREFIX = "following:";

    public static String buildFollowUserZsetKey(Long userId) {
        return FOLLOW_USER_ZSET_KEY_PREFIX + userId;
    }

    private final static String USER_FANS_ZSET_KEY_PREFIX = "fans:";

    public static String buildUserFansZsetKey(Long followingUserId) {
        return  USER_FANS_ZSET_KEY_PREFIX + followingUserId;
    }
}
