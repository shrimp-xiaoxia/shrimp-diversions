package com.shrimp.user.dto.req;

import com.shrimp.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FindUserByPhoneReqDTO {

    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;
}
