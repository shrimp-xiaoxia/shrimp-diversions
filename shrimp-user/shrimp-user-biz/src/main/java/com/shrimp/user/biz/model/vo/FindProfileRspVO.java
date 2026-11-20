package com.shrimp.user.biz.model.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FindProfileRspVO {
    private String userId;
    private String nickname;
    private String avatar;
    private String xiaohashuId;
    private Byte sex;
    private LocalDate birthday;
    private Integer age;
    private String introduction;
    private Long fansTotal;
    private Long followingTotal;
    private Long noteTotal;
    private Long likeAndCollectTotal;
    private boolean fansTotalAvailable;
    private boolean followingTotalAvailable;
    private boolean noteTotalAvailable;
    private boolean likeAndCollectTotalAvailable;
}
