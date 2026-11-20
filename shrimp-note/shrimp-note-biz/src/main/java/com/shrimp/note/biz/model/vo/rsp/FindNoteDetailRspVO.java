package com.shrimp.note.biz.model.vo.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.shrimp.framework.common.annotation.BrowserId;

/**
 * @author ccb
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNoteDetailRspVO {
    @BrowserId
    private Long id;

    private Integer type;

    private String title;

    private String content;

    private List<String> imgUris;

    @BrowserId
    private Long topicId;

    private String topicName;

    @BrowserId
    private Long creatorId;

    private String creatorName;

    private String avatar;

    private String videoUri;

    /**
     * 编辑时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否可见
     */
    private Integer visible;
    private Long likeTotal;
    private Long collectTotal;
    private Long commentTotal;
    private boolean likeTotalAvailable;
    private boolean collectTotalAvailable;
    private boolean commentTotalAvailable;
}
