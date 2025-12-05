package com.shrimp.note.biz.domain.mapper;


import com.shrimp.note.biz.domain.dataobject.NoteCollectionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteCollectionDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NoteCollectionDO record);

    int insertSelective(NoteCollectionDO record);

    NoteCollectionDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteCollectionDO record);

    int updateByPrimaryKey(NoteCollectionDO record);

    int selectCollectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteCollectionDO> selectByUserId(Long userId);

    List<NoteCollectionDO> selectCollectByNoteIdAndLimit(@Param("userId") Long userId,@Param("limit") Long limit);

    int insertOrUpdate(NoteCollectionDO noteCollectionDO);

    int updateUnCollect(NoteCollectionDO noteCollectionDO);
}