package com.shrimp.user.relation.biz.domain.mapper;

import com.shrimp.user.relation.biz.domain.dataobject.FollowingDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowingDOMapper {
    List<Long> selectFollowingIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
    long countAvailable(@Param("userId") Long userId);
    List<Long> pageAvailable(@Param("userId") Long userId, @Param("offset") long offset);
    int deleteByPrimaryKey(Long id);

    int insert(FollowingDO record);

    int insertSelective(FollowingDO record);

    FollowingDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FollowingDO record);

    int updateByPrimaryKey(FollowingDO record);

    List<FollowingDO> selectByUserId(Long userId);

    int deleteByUserIdAndFollowingUserId(@Param("userId") Long userId, @Param("followingUserId") Long followingUserId);

    long selectFollowCountByUserId(Long userId);

    List<FollowingDO> selectPageListByUserId(@Param("userId") Long userId,
                                             @Param("offset") Long offset
            , @Param("limit") Long limit);

    //    需要回表查询
    List<FollowingDO> selectAllFollowingByUserId(Long userId);
}
