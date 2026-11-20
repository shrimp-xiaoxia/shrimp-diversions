package com.shrimp.note.biz.model.vo.req;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class ProfileNotesReqVO {
    @NotNull private Long userId;
    @NotNull @Min(1) @Max(3) private Integer type;
    @NotNull @Min(1) private Integer pageNo = 1;
}
