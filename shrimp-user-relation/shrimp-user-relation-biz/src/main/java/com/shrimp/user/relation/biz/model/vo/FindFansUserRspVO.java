package com.shrimp.user.relation.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.shrimp.framework.common.annotation.BrowserId;

/**
 * @author ccb
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFansUserRspVO {

    @BrowserId
    private Long userId;
    private Boolean isLiked;
    private boolean followStateAvailable;
    private boolean fansTotalAvailable;
    private boolean noteTotalAvailable;

    private String avatar;

    private String nickName;

    private Long fansTotal;

    private Long noteTotal;
}
