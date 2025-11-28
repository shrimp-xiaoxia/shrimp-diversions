package com.shrimp.user.relation.biz.model.vo;

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
public class FindFansUserRspVO {

    private Long userId;

    private String avatar;

    private String nickName;

    private Long fansTotal;

    private Long noteTotal;
}
