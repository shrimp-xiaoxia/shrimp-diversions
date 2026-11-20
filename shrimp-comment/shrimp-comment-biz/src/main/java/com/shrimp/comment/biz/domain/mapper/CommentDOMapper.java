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

    /**
     * 批量插入
     *
     * @param comments
     * @return
     */
    int batchInsert(@Param("comments") List<CommentBO> comments);

    /**
     * 批量查询批量
     *
     * @param commentIds
     * @return
     */
    List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 批量更新评论热度
     *
     * @param commentIds
     * @param commentHeatBOS
     * @return
     */
    int batchUpdateHeatByCommentIds(@Param("commentIds") List<Long> commentIds, @Param("commentHeatBOS") List<CommentHeatBO> commentHeatBOS);

    /**
     * 批量查询一级评论下最早回复的评论
     *
     * @param parentIds
     * @return
     */
    List<CommentDO> batchSelectEarliestByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 批量更新一级评论的首评论
     *
     * @param CommentDOs
     * @param ids
     * @return
     */
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

    long countTopLevelByNoteId(@Param("noteId") Long noteId);

    /**
     * 批量查询二级评论
     *
     * @param commentIds
     * @return
     */
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

    /**
     * 批量查询批量
     *
     * @param commentIds
     * @return
     */
    List<CommentDO> selectCommentCountByIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 定量查询子评论
     *
     * @param parentId
     * @param limit
     * @return
     */
    List<CommentDO> selectChildCommentByParentIdAndLimit(@Param("parentId") Long parentId,
                                                         @Param("limit") Long limit);

    /**
     * 删除一级评论下的所有评论
     *
     * @param parentId
     * @return
     */
    int deleteChildCommentByParentId(Long parentId);

    /**
     * 查询子评论ID集合
     *
     * @param parentId
     * @return
     */
    List<Long> selectChildCommentIdByParentId(Long parentId);

    /**
     * 批量删除评论
     *
     * @param commentIds
     * @return
     */
    Long batchDeleteCommentByIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 查询回复此评论的评论ID集合
     *
     * @param commentId
     * @return
     */
    List<Long> selectReplyIdByBeRepliedId(Long commentId);

    /**
     * 更新一级评论的子评论总数
     *
     * @param commentId
     * @param count
     * @return
     */
    int updateChildCommentTotalById(@Param("commentId") Long commentId, @Param("count") Long count);

    int updateFirstReplyAfterDelete(@Param("parentId") Long parentId, @Param("firstReplyId") Long firstReplyId);

}
