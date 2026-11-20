package com.shrimp.comment.biz.model.vo;

import cn.hutool.core.date.DateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.shrimp.framework.common.annotation.BrowserId;

/**
 * @author ccb
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FindChildCommentItemRspVO {

    @BrowserId
    private Long commentId;

    @BrowserId
    private Long userId;

    private String avatar;

    private String nickname;

    private String content;

    private String imageUrl;

    private String createTime;

    private Long likeTotal;

    private String replyUserName;

    @BrowserId
    private Long replyUserId;
    private Boolean isLiked;
    private boolean likeStateAvailable;
}
