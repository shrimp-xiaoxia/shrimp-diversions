package com.shrimp.note.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ccb
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LikeUnlikeNoteMqDTO {

    private Long userId;
    private Long noteId;
    private Integer type;
    private LocalDateTime createTime;
    private Long noteCreatorId;
}
