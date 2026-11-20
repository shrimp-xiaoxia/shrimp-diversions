package com.shrimp.note.biz.domain.mapper;

import com.shrimp.note.biz.domain.dataobject.NoteDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface NoteDOMapper {
    long countDiscover(@Param("channelId") Long channelId);
    List<NoteDO> findDiscover(@Param("channelId") Long channelId, @Param("offset") long offset);
    long countProfile(@Param("userId") Long userId, @Param("type") int type, @Param("owner") boolean owner);
    List<NoteDO> findProfile(@Param("userId") Long userId, @Param("type") int type, @Param("owner") boolean owner, @Param("offset") long offset);
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
