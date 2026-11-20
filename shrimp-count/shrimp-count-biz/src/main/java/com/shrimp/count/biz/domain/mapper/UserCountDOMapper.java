package com.shrimp.count.biz.domain.mapper;

import com.shrimp.count.biz.domain.dataobject.UserCountDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserCountDOMapper {
    List<UserCountDO> selectByUserIds(@Param("ids") List<Long> ids);
    int deleteByPrimaryKey(Long id);

    int insert(UserCountDO record);

    int insertSelective(UserCountDO record);

    UserCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserCountDO record);

    int updateByPrimaryKey(UserCountDO record);

    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateLikesTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateCollectsTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateNoteTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);
}
