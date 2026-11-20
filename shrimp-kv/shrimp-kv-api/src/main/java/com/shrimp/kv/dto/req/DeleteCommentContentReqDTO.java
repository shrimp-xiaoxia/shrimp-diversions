package com.shrimp.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class DeleteCommentContentReqDTO {
    @NotNull(message = "笔记ID不能为空")
    private Long noteId;
    @NotBlank(message = "发布年月不能为空")
    private String yearMonth;
    @NotBlank(message = "评论ID不能为空")
    private String commentId;
}
