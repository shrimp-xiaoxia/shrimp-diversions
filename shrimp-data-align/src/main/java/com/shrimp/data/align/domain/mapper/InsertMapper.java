package com.shrimp.data.align.domain.mapper;


import org.apache.ibatis.annotations.Param;

/**
 * 添加记录
 */
public interface InsertMapper {

    /**
     * 笔记点赞数：计数变更
     */
    void insert2DataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);

    /**
     * 用户获得的点赞数：计数变更
     */
    void insert2DataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /**
     * 笔记点赞数：计数变更
     */
    void insert2DataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);

    /**
     * 用户获得的点赞数：计数变更
     */
    void insert2DataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /**
     * 用户获得的发布笔记数：计数变更
     */
    void insert2DataAlignUserNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /**
     * 用户获得的关注数：计数变更
     */
    void insert2DataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    /**
     * 用户获得的关注数：计数变更
     */
    void insert2DataAlignFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);


}
