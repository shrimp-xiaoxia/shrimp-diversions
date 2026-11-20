package com.shrimp.comment.biz.constants;

public interface MQConstants {
    /**
     * Topic: 评论发布
     */
    String TOPIC_PUBLISH_COMMENT = "PublishCommentTopic";

    /**
     * Topic: 笔记评论总数计数
     */
    String TOPIC_COUNT_NOTE_COMMENT = "CountNoteCommentTopic";

    /**
     * Topic: 评论热度值更新
     */
    String TOPIC_COMMENT_HEAT_UPDATE = "CommentHeatUpdateTopic";

    /**
     * Topic:评论点赞、取消点赞共用一个Topic
     */
    String TOPIC_COMMENT_LIKE_OR_UNLIKE = "CommentLikeOrUnlikeTopic";

    /**
     * tag:评论点赞
     */
    String TAG_LIKE = "Like";
    /**
     * tag:取消评论点赞
     */
    String TAG_UNLIKE = "Unlike";

    /**
     * Topic: 延长删除评论缓存
     */
    String TOPIC_COMMENT_DELAY_DELETE_CACHE = "CommentDelayDeleteCache";

    String TOPIC_DELETE_COMMENT = "DeleteCommentTopic";
}
