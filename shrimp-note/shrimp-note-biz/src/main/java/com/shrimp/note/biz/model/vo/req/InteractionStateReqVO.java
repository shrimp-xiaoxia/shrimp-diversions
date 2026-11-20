package com.shrimp.note.biz.model.vo.req;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class InteractionStateReqVO {
    @NotNull private Long noteId;
}
