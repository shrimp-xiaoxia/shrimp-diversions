package com.shrimp.user.dto.resp;

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
public class FindUserByIdRspDTO {

    private Long id;

    private String nickName;

    private String avatar;

    private String introduction;
}
