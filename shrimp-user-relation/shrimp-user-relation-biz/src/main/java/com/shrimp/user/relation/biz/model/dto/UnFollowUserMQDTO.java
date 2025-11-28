package com.shrimp.user.relation.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnFollowUserMQDTO {
    private Long userId;

    private Long unFollowUserId;

    private LocalDateTime createTime;
}
