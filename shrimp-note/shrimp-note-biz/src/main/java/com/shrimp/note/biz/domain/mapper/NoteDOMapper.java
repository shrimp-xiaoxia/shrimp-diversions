package com.shrimp.note.biz.domain.mapper;

import com.shrimp.note.biz.domain.dataobject.NoteDO;

public interface NoteDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NoteDO record);

    int insertSelective(NoteDO record);

    NoteDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteDO record);

    int updateByPrimaryKey(NoteDO record);

    int updateNoteVisible(NoteDO record);

    int updateNoteIsTop(NoteDO record);

    int selectCountByNoteId(Long noteId);

    Long selectCreatorIdByNoteId(Long noteId);
}