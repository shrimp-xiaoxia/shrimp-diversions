package com.shrimp.kv.dto.rsp;

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
public class FindNoteContentRspDTO {

    private String uuid;
    private String content;
}
