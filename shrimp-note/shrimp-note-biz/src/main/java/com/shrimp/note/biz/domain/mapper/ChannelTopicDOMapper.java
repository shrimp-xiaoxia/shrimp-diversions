package com.shrimp.note.biz.domain.mapper;

import com.shrimp.note.biz.domain.dataobject.ChannelTopicDO;

public interface ChannelTopicDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ChannelTopicDO record);

    int insertSelective(ChannelTopicDO record);

    ChannelTopicDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelTopicDO record);

    int updateByPrimaryKey(ChannelTopicDO record);
}