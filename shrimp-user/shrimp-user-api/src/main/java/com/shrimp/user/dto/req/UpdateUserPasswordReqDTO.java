package com.shrimp.user.dto.req;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateUserPasswordReqDTO {
    @NotBlank(message = "密码不能为空")
    private String encodePassword;
}
