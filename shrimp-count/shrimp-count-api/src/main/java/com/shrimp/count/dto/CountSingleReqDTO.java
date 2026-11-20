package com.shrimp.count.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class CountSingleReqDTO {
    @NotNull private Long id;
}
