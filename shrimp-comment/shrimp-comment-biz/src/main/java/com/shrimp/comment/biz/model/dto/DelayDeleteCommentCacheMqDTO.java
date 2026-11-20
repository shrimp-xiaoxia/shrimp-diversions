package com.shrimp.comment.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ccb
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DelayDeleteCommentCacheMqDTO {
    private Long commentId;

    private Integer level;

    private Long noteId;

    private Long parentId;
}
