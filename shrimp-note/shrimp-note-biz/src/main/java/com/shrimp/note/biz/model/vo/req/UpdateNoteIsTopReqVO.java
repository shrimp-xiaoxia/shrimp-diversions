package com.shrimp.note.biz.model.vo.req;

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
public class UpdateNoteIsTopReqVO {

    @NotNull(message = "笔记ID不能为空")
    private Long id;

    @NotNull(message = "需要指定是否置顶")
    private Boolean isTop;
}
