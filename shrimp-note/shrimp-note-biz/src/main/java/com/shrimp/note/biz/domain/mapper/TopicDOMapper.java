package com.shrimp.note.biz.domain.mapper;

import com.shrimp.note.biz.domain.dataobject.TopicDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface TopicDOMapper {
    List<TopicDO> searchEnabled(@Param("keyword") String keyword);
    TopicDO findEnabledByName(@Param("name") String name);
    int insertTopic(@Param("name") String name);
    int deleteByPrimaryKey(Long id);

    int insert(TopicDO record);

    int insertSelective(TopicDO record);

    TopicDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TopicDO record);

    int updateByPrimaryKey(TopicDO record);

    String selectTopicNameById(Long id);
}
