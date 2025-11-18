package com.shrimp.user.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FindUserByIdReqDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;
}
