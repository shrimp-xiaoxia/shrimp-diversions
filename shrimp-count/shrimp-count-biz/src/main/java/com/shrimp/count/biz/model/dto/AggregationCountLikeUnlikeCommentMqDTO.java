package com.shrimp.count.biz.model.dto;

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
public class AggregationCountLikeUnlikeCommentMqDTO {
    private Long commentId;
    private Integer count;
}
