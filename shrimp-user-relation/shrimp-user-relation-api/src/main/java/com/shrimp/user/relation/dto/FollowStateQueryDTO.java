package com.shrimp.user.relation.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
@Data
public class FollowStateQueryDTO {
    @NotNull private Long userId;
    @NotEmpty @Size(max = 100) private List<@NotNull Long> ids;
}
