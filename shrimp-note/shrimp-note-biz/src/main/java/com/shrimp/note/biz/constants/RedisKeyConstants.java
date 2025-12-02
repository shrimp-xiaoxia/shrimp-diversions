package com.shrimp.note.biz.constants;

/**
 * @author ccb
 */
public class RedisKeyConstants {
    private static final String NOTE_DETAIL_CACHE_KEY_PREFIX = "note:detail:cache:";
    /**
     * 布隆过滤器：用户笔记点赞
     */
    public static final String BLOOM_USER_NOTE_LIKE_LIST_KEY = "bloom:note:likes:";

    /**
     * 用户笔记点赞列表 ZSet 前缀
     */
    public static final String USER_NOTE_LIKE_ZSET_KEY = "user:note:likes:";

    public static String buildNoteDetailCacheKey(Long noteId) {
        return NOTE_DETAIL_CACHE_KEY_PREFIX + noteId;
    }

    /**
     * 构建完整的布隆过滤器：用户笔记点赞 KEY
     *
     * @param userId
     * @return
     */
    public static String buildBloomUserNoteLikeListKey(Long userId) {
        return BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
    }

    /**
     * 构建完整的用户笔记点赞列表 ZSet KEY
     * @param userId
     * @return
     */
    public static String buildUserNoteLikeZSetKey(Long userId) {
        return USER_NOTE_LIKE_ZSET_KEY + userId;
    }
}
