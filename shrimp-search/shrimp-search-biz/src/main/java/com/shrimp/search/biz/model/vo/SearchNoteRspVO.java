package com.shrimp.search.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.shrimp.framework.common.annotation.BrowserId;

import java.time.LocalDateTime;

/**
 * @author ccb
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SearchNoteRspVO {
    @BrowserId
    private Long noteId;
    private String cover;
    private String title;
    private String highlightTitle;
    private String avatar;
    private String nickname;
    @BrowserId
    private Long creatorId;
    private Integer type;
    private String videoUri;
    private String updateTime;
    private String likeCount;
    /**
     * 被评论数
     */
    private String commentTotal;

    /**
     * 被收藏数
     */
    private String collectTotal;
}
