package com.shrimp.user.relation.biz.model.vo;

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
public class FindFollowingListReqVO {

    @NotNull(message = "查询用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "页码不能为空")
    // 默认值为第一页
    private Integer pageNo = 1;
}