package com.shrimp.note.biz.domain.mapper;


import com.shrimp.note.biz.domain.dataobject.NoteLikeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteLikeDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NoteLikeDO record);

    int insertSelective(NoteLikeDO record);

    NoteLikeDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteLikeDO record);

    int updateByPrimaryKey(NoteLikeDO record);

    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteLikeDO> selectByUserId(Long userId);

    List<NoteLikeDO> selectLikedByUserIdAndLimit(@Param("userId") Long userId,@Param("limit") Long limit);

    int insertOrUpdate(NoteLikeDO noteLikeDO);

    int updateUnlikeNoteByUserIdAndNoteId(NoteLikeDO noteLikeDO);

    int batchInsertOrUpdate(@Param("noteLikeDOS") List<NoteLikeDO> noteLikeDOS);
}