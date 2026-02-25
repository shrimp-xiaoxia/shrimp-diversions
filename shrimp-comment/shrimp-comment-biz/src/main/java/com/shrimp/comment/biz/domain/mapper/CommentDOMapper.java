package com.shrimp.comment.biz.domain.mapper;

import com.shrimp.comment.biz.domain.bo.CommentBO;
import com.shrimp.comment.biz.domain.bo.CommentHeatBO;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CommentDO record);

    int insertSelective(CommentDO record);

    CommentDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CommentDO record);

    int updateByPrimaryKey(CommentDO record);

    int batchInsert(@Param("comments") List<CommentBO> comments);

    List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

    int batchUpdateHeatByCommentIds(@Param("commentIds") List<Long> commentIds, @Param("commentHeatBOS") List<CommentHeatBO> commentHeatBOS);

    List<CommentDO> batchSelectEarliestByParentIds(@Param("parentIds") List<Long> parentIds);

    int batchUpdateFirstReplyCommentIdByPrimaryKey(@Param("commentDOs") List<CommentDO> CommentDOs,
                                                   @Param("ids") List<Long> ids);

    /**
     * 查询评论分页数据
     *
     * @param noteId
     * @param offset
     * @param pageSize
     * @return
     */
    List<CommentDO> selectPageList(@Param("noteId") Long noteId,
                                   @Param("offset") long offset,
                                   @Param("pageSize") long pageSize);

    List<CommentDO> selectTwoLevelCommentByIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 查询热门评论
     *
     * @param noteId
     * @return
     */
    List<CommentDO> selectHeatComments(Long noteId);

    /**
     * 查询一级评论下的子评论总数
     *
     * @param commentId
     * @return
     */
    Long selectChildCommentTotalById(Long commentId);

    /**
     * 查询二级评论分页数据
     *
     * @param parentId
     * @param offset
     * @param pageSize
     * @return
     */
    List<CommentDO> selectChildPageList(@Param("parentId") Long parentId,
                                        @Param("offset") long offset,
                                        @Param("pageSize") long pageSize);
}