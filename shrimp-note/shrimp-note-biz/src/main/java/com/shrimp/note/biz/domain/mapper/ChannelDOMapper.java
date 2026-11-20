package com.shrimp.note.biz.domain.mapper;

import com.shrimp.note.biz.domain.dataobject.ChannelDO;
import java.util.List;

public interface ChannelDOMapper {
    List<ChannelDO> selectEnabled();
    int deleteByPrimaryKey(Long id);

    int insert(ChannelDO record);

    int insertSelective(ChannelDO record);

    ChannelDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelDO record);

    int updateByPrimaryKey(ChannelDO record);
}
