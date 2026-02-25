package com.shrimp.count.biz.domain.mapper;

import com.shrimp.count.biz.domain.dataobject.CommentDO;
import org.apache.ibatis.annotations.Param;

public interface CommentDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CommentDO record);

    int insertSelective(CommentDO record);

    CommentDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CommentDO record);

    int updateByPrimaryKey(CommentDO record);

    int updateChildCommentTotalInt(@Param("commentId") Long commentId, @Param("childCommentTotal") Integer childCommentTotal);
}