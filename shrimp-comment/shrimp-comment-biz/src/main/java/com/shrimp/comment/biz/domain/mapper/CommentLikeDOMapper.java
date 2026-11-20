package com.shrimp.comment.biz.domain.mapper;

import com.shrimp.comment.biz.domain.dataobject.CommentLikeDO;
import com.shrimp.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentLikeDOMapper {
    List<Long> selectLikedCommentIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
    int deleteByPrimaryKey(Long id);

    int insert(CommentLikeDO record);

    int insertSelective(CommentLikeDO record);

    CommentLikeDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CommentLikeDO record);

    int updateByPrimaryKey(CommentLikeDO record);

    Long selectCountByCommentIdAndUserId(@Param("commentId") Long commentId,
                                         @Param("userId") Long userId);

    List<Long> selectUserIdsByCommentId(Long commentId);

    List<Long> selectCommentIdsByUserId(Long userId);

    int batchDelete(@Param("unlikes") List<LikeUnlikeCommentMqDTO> unlikes);

    int batchInsert(@Param("likes") List<LikeUnlikeCommentMqDTO> likes);
}
