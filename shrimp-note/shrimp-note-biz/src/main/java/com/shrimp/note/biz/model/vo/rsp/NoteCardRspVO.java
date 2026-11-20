package com.shrimp.note.biz.model.vo.rsp;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data
public class NoteCardRspVO {
    private String id;
    private Integer type;
    private String title;
    private String cover;
    private String videoUri;
    private String creatorId;
    private String nickname;
    private String avatar;
    private Long likeTotal;
    private boolean likeTotalAvailable;
    @JsonProperty("isLiked")
    private Boolean isLiked;
    private Integer visible;
}
