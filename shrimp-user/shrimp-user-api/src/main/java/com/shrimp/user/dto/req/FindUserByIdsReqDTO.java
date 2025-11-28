package com.shrimp.user.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ccb
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FindUserByIdsReqDTO {
    @NotNull(message = "用户ID集合不能为空")
    @Size(min = 1,max = 10,message = "用户ID集合的大小必须大于1，和小于等于10")
    private List<Long> ids;
}
