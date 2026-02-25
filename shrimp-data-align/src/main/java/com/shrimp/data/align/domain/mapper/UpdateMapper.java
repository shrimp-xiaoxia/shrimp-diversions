package com.shrimp.data.align.domain.mapper;


import org.apache.ibatis.annotations.Param;

public interface UpdateMapper {

    /**
     * 更新 t_user_count 计数表总关注数
     */
    int updateUserFollowingTotalByUserId(@Param("userId") long userId,
                                         @Param("followingTotal") int followingTotal);

    /**
     * 更新 t_note_count 计数表笔记点赞数
     */
    int updateNoteLikeTotalByNoteId(@Param("noteId") long noteId,
                                    @Param("noteLikeTotal") int noteLikeTotal);
}
