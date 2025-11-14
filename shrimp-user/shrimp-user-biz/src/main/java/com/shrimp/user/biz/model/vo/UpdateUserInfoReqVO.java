package com.shrimp.user.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateUserInfoReqVO {
    private MultipartFile avatar;

    private LocalDate birthday;

    private String nickName;

    private String xiaohashuId;

    private Integer sex;

    private String introduction;

    private MultipartFile backgroundImg;
}
