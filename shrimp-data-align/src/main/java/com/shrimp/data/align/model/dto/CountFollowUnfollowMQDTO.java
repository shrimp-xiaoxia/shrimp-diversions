package com.shrimp.data.align.model.dto;

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
public class CountFollowUnfollowMQDTO {

    private Long userId;
    private Long targetUserId;
    /**
     * 1:关注 0:取关
     */
    private Integer type;
}
