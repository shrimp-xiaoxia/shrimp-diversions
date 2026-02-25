package com.shrimp.search.biz.model.vo;

import jakarta.validation.constraints.Min;
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
public class SearchUserReqVO {
    @NotBlank(message = "关键词不能为空")
    private String keyword;
    @Min(value = 1, message = "页码不能小于 1")
    // 默认值为第一页
    private Integer pageNo= 1;
}
