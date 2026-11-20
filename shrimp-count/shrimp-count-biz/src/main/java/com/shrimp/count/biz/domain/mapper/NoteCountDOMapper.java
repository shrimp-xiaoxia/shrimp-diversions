package com.shrimp.count.biz.domain.mapper;

import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface NoteCountDOMapper {
    List<NoteCountDO> selectByNoteIds(@Param("ids") List<Long> ids);
    int deleteByPrimaryKey(Long id);

    int insert(NoteCountDO record);

    int insertSelective(NoteCountDO record);

    NoteCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteCountDO record);

    int updateByPrimaryKey(NoteCountDO record);

    int insertOrUpdateLikeCountByNoteId(NoteCountDO noteCountDO);

    int insertOrUpdateCollectCountByNoteId(NoteCountDO noteCountDO);

    int insertOrUpdateCommentCountByNoteId(@Param("noteId") Long noteId, @Param("commentTotal") Integer commentTotal);
}
