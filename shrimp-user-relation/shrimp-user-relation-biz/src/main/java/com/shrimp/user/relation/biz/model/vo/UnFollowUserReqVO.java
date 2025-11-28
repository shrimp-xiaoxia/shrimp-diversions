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
public class UnFollowUserReqVO {

    @NotNull(message = "用户ID不能为空")
    private Long unfollowUserId;
}
