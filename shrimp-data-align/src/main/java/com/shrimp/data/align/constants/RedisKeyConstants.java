package com.shrimp.data.align.constants;

public class RedisKeyConstants {
    /**
     * 布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞（笔记ID） 前缀
     */
    public static final String BLOOM_TODAY_NOTE_LIKE_NOTE_ID_LIST_KEY = "bloom:dataAlign:note:like:noteIds:";

    /**
     * 布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞（笔记发布者ID） 前缀
     */
    public static final String BLOOM_TODAY_NOTE_LIKE_USER_ID_LIST_KEY = "bloom:dataAlign:note:like:userIds:";

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞(笔记ID) KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserNoteLikeNoteIdListKey(String date) {
        return BLOOM_TODAY_NOTE_LIKE_NOTE_ID_LIST_KEY + date;
    }

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞(笔记发布者ID) KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserNoteLikeUserIdListKey(String date) {
        return BLOOM_TODAY_NOTE_LIKE_USER_ID_LIST_KEY + date;
    }

    /**
     * 布隆过滤器：日增量变更数据，用户笔记收藏，取消收藏（笔记ID） 前缀
     */
    public static final String BLOOM_TODAY_NOTE_COLLECT_NOTE_ID_LIST_KEY = "bloom:dataAlign:note:collect:noteIds:";

    /**
     * 布隆过滤器：日增量变更数据，用户笔记收藏，取消收藏（笔记发布者ID） 前缀
     */
    public static final String BLOOM_TODAY_NOTE_COLLECT_USER_ID_LIST_KEY = "bloom:dataAlign:note:collect:userIds:";

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户笔记收藏，取消收藏(笔记ID) KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserNoteCollectNoteIdListKey(String date) {
        return BLOOM_TODAY_NOTE_COLLECT_NOTE_ID_LIST_KEY + date;
    }

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞(笔记发布者ID) KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserNoteCollectUserIdListKey(String date) {
        return BLOOM_TODAY_NOTE_COLLECT_USER_ID_LIST_KEY + date;
    }

    /**
     * 布隆过滤器：日增量变更数据，用户笔记发布，删除笔记（笔记发布者ID） 前缀
     */
    public static final String BLOOM_TODAY_NOTE_OPERATE_USER_ID_LIST_KEY = "bloom:dataAlign:note:operate:userIds:";

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户笔记点赞，取消点赞(笔记发布者ID) KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserNoteOperateUserIdListKey(String date) {
        return BLOOM_TODAY_NOTE_OPERATE_USER_ID_LIST_KEY + date;
    }

    /**
     * 布隆过滤器：日增量变更数据，用户关注数(该用户ID)前缀
     */
    public static final String BLOOM_TODAY_FOLLOWING_USER_ID_LIST_KEY = "bloom:dataAlign:user:following:";

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户关注数(该用户ID)KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserRelationFollowingUserIdListKey(String date) {
        return BLOOM_TODAY_FOLLOWING_USER_ID_LIST_KEY + date;
    }

    /**
     * 布隆过滤器：日增量变更数据，用户粉丝数(该用户ID)前缀
     */
    public static final String BLOOM_TODAY_FANS_USER_ID_LIST_KEY = "bloom:dataAlign:user:fans:";

    /**
     * 构建完整的布隆过滤器：日增量变更数据，用户粉丝数(该用户ID)KEY
     *
     * @param date
     * @return
     */
    public static String buildBloomUserRelationFansUserIdListKey(String date) {
        return BLOOM_TODAY_FANS_USER_ID_LIST_KEY + date;
    }


    /**
     * 用户维度计数 Key 前缀
     */
    private static final String COUNT_USER_KEY_PREFIX = "count:user:";

    /**
     * Hash Field: 粉丝总数
     */
    public static final String FIELD_FANS_TOTAL = "fansTotal";

    public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";

    /**
     * Hash Field: 笔记发布总数
     */
    public static final String FIELD_NOTE_TOTAL = "noteTotal";
    /**
     * 构建用户维度计数 Key
     *
     * @param userId
     * @return
     */
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

    /**
     * 笔记维度计数 Key 前缀
     */
    private static final String COUNT_NOTE_KEY_PREFIX = "count:note:";

    /**
     * Hash Field: 笔记点赞总数
     */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";

    /**
     * Hash Field: 笔记点赞总数
     */
    public static final String FIELD_COLLECT_TOTAL = "collectTotal";

    /**
     * 构建笔记维度计数 Key
     *
     * @param noteId
     * @return
     */
    public static String buildCountNoteKey(Long noteId) {
        return COUNT_NOTE_KEY_PREFIX + noteId;
    }

}
