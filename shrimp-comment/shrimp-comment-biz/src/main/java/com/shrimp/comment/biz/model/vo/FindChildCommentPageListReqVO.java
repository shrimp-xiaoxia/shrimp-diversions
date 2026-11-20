package com.shrimp.comment.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ccb
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FindChildCommentPageListReqVO {
    @NotNull(message = "父评论ID不能为空")
    private Long parentCommentId;

    @NotNull(message = "页码不能为空")
    private Integer pageNo = 1;

    /** 是否包含首条回复；未传时保持原先跳过首条的分页语义。 */
    private Boolean includeFirst;
}
