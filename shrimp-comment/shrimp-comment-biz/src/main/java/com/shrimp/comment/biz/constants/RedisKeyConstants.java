package com.shrimp.comment.biz.constants;

/**
 * @author ccb
 */
public class RedisKeyConstants {

    /**
     * Key 前缀：一级评论的 first_reply_comment_id 字段值是否更新标识
     */
    private static final String HAVE_FIRST_REPLY_COMMENT_KEY_PREFIX = "comment:havaFirstReplyCommentId:";

    /**
     * 构建完整 KEY
     *
     * @param
     * @return
     */
    public static String buildHaveFirstReplyCommentKey(Long commentId) {
        return HAVE_FIRST_REPLY_COMMENT_KEY_PREFIX + commentId;
    }

    private static final String COUNT_NOTE_TOTAL_KEY_PREFIX = "count:note:";

    public static final String FIELD_COMMENT_TOTAL = "commentTotal";

    public static String buildCountNoteTotalKey(Long noteId) {
        return COUNT_NOTE_TOTAL_KEY_PREFIX + noteId;
    }

    private static final String COMMENT_LIST_KEY_PREFIX = "comment:list:";

    public static String buildCommentListKey(Long noteId) {
        return COMMENT_LIST_KEY_PREFIX + noteId;
    }

    private static final String COMMENT_DETAIL_KEY_PREFIX = "comment:detail:";

    public static String buildCommentDetailKey(Long commentId) {
        return COMMENT_DETAIL_KEY_PREFIX + commentId;
    }

    private static final String CHILD_COMMENT_DETAIL_KEY_PREFIX = "childComment:detail:";

    public static String buildChildCommentDetailKey(Long commentId) {
        return CHILD_COMMENT_DETAIL_KEY_PREFIX + commentId;
    }


    private static final String COUNT_COMMENT_KEY_PREFIX = "count:comment:";

    /**
     * Hash Field: 子评论总数
     */
    public static final String FIELD_CHILD_COMMENT_TOTAL = "childCommentTotal";

    /**
     * Hash Field: 点赞总数
     */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";

    /**
     * 构建评论相关计数KEY
     *
     * @param commentId
     * @return
     */
    public static String buildCountCommentKey(Long commentId) {
        return COUNT_COMMENT_KEY_PREFIX + commentId;
    }

    private static final String CHILD_COMMENT_LIST_KEY_PREFIX = "comment:childList:";

    /**
     * 构建子评论分页zset完整KEY
     *
     * @param commentId
     * @return
     */
    public static String buildChildCommentListKey(Long commentId) {
        return CHILD_COMMENT_LIST_KEY_PREFIX + commentId;
    }

    private static final String BLOOM_COMMENT_LIKE_KEY_PREFIX = "bloom:comment:like:";

    /**
     * 构建评论点赞KEY
     *
     * @param userId 用户 ID
     * @return
     */
    public static String buildBloomCommentLikeKey(Long userId) {
        return BLOOM_COMMENT_LIKE_KEY_PREFIX + userId;
    }
}
