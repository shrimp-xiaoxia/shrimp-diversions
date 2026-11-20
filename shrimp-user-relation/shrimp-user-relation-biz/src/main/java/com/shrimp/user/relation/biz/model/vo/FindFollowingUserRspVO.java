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
public class FindFollowingUserRspVO {

    @BrowserId
    private Long userId;
    private Boolean isLiked;
    private boolean followStateAvailable;

    private String avatar;

    private String nickname;

    private String introduction;

}
