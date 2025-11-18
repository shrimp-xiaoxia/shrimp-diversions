package com.shrimp.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
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
public class DeleteNoteContentReqDTO {

    @NotBlank(message = "笔记ID不能为空")
    private String uuid;
}
