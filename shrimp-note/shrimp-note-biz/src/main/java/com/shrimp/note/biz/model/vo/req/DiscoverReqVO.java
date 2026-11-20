package com.shrimp.note.biz.model.vo.req;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class DiscoverReqVO {
    private Long channelId;
    @NotNull @Min(1) private Integer pageNo = 1;
}
