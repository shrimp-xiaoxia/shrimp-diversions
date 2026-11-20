package com.shrimp.count.dto;

import lombok.Data;

@Data
public class CountResultDTO {
    private Long id;
    private Long likeTotal;
    private Long collectTotal;
    private Long commentTotal;
    private Long fansTotal;
    private Long followingTotal;
    private Long noteTotal;
}
