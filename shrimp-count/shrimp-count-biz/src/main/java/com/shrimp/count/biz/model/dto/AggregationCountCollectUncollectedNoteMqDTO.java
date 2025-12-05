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
public class AggregationCountCollectUncollectedNoteMqDTO {

    private Long noteId;
    private Long noteCreatorId;
    private Integer count;
}
