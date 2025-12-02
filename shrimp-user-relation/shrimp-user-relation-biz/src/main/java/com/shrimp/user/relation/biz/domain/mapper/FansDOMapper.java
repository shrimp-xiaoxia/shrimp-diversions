package com.shrimp.user.relation.biz.domain.mapper;

import com.shrimp.user.relation.biz.domain.dataobject.FansDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FansDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FansDO record);

    int insertSelective(FansDO record);

    FansDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FansDO record);

    int updateByPrimaryKey(FansDO record);

    int deleteByUserIdAndFansUserId(@Param("userId") Long userId, @Param("fansUserId") Long fansUserId);

    long selectFansCountByUserId(Long userId);

    List<FansDO> selectPageListByUserId(@Param("userId") Long userId, @Param("offset") Long offset,@Param("limit") Long limit);

    List<FansDO> selectMax5000ByUserId(Long userId);
}