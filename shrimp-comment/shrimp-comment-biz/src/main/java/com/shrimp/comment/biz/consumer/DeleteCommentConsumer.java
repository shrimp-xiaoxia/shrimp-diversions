package com.shrimp.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.model.dto.DelayDeleteCommentCacheMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_DELETE_COMMENT, // Group 组
        topic = MQConstants.TOPIC_DELETE_COMMENT// 主题 Topic
)
@Slf4j
public class DeleteCommentConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    //    每秒创建1000个令牌
    private RateLimiter rateLimiter = RateLimiter.create(1000);


    @Override
    public void onMessage(String body) {
        // 令牌桶流控
        rateLimiter.acquire();
        log.info("## 【删除评论 - 后续业务处理】消费者消费成功, body: {}", body);
        CommentDO commentDO = null;
        try {
            commentDO = JsonUtils.parseJSONString(body, CommentDO.class);
        } catch (Exception e) {
            log.error("解析Json字符串失败");
        }

        Integer level = commentDO.getLevel();
        CommentLevelEnum commentLevelEnum = CommentLevelEnum.valueOf(level);

        switch (commentLevelEnum) {
            case ONE -> handleOneLevelComment(commentDO);
            case TWO -> handleTwoLevelComment(commentDO);
        }

    }

    /**
     * 一级评论处理
     *
     * @param commentDO
     */
    private void handleOneLevelComment(CommentDO commentDO) {
        Long parentId = commentDO.getId();
//        开启事务

//        TODO 删除用户和评论的点赞中间表

//        关联评论删除(一级评论下所有的子评论，都需要删除)
        int deletedCount = commentDOMapper.deleteChildCommentByParentId(parentId);
//        计数更新（笔记下的总评论数）
        Long noteId = commentDO.getNoteId();
        String countNoteTotalKey = RedisKeyConstants.buildCountNoteTotalKey(noteId);
        Boolean hasKey = redisTemplate.hasKey(countNoteTotalKey);
        if (hasKey) {
            redisTemplate.opsForHash().increment(countNoteTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, -(deletedCount + 1));
        }

        // 更新 t_note_count 计数表
        noteCountDOMapper.updateCommentTotalByNoteId(noteId, -(long) (deletedCount + 1));
    }

    /**
     * 二级评论处理
     *
     * @param commentDO
     */
    private void handleTwoLevelComment(CommentDO commentDO) {
//        开启事务
        Long needRemoveChildCommentId = commentDO.getId();
//        批量删除关联评论
        List<Long> commentIds = Lists.newArrayList();
        getReplyCommentIds(commentIds, needRemoveChildCommentId);
//        TODO 删除用户和评论的点赞中间表
        Long parentId = commentDO.getParentId();
        long count = commentIds.isEmpty() ? 0L : commentDOMapper.batchDeleteCommentByIds(commentIds);
        long deletedTotal = 1L + count;
        commentDOMapper.updateChildCommentTotalById(parentId, -deletedTotal);
        noteCountDOMapper.updateCommentTotalByNoteId(commentDO.getNoteId(), -deletedTotal);
        redisTemplate.delete(RedisKeyConstants.buildCountCommentKey(parentId));
        redisTemplate.delete(RedisKeyConstants.buildCountNoteTotalKey(commentDO.getNoteId()));
        redisTemplate.delete(RedisKeyConstants.buildChildCommentListKey(parentId));
        redisTemplate.delete(RedisKeyConstants.buildCommentDetailKey(parentId));

//        若最早回复被删除（包括级联删除），重新选择首条回复
        CommentDO parentCommentDO = commentDOMapper.selectByPrimaryKey(parentId);
        if (parentCommentDO == null) {
            log.info("一级评论已不存在，跳过首条回复和热度更新，parentId: {}", parentId);
            return;
        }
        Long firstReplyCommentId = parentCommentDO.getFirstReplyCommentId();
        if (ObjUtil.equals(needRemoveChildCommentId, firstReplyCommentId) || commentIds.contains(firstReplyCommentId)) {
            List<CommentDO> commentDOS = commentDOMapper.batchSelectEarliestByParentIds(Collections.singletonList(parentId));
            Long newFirstReplyCommentId = 0L;
            if (CollUtil.isNotEmpty(commentDOS)) {
                CommentDO newFirstReplyComment = commentDOS.get(0);
                newFirstReplyCommentId = newFirstReplyComment.getId();
            }
            commentDOMapper.updateFirstReplyAfterDelete(parentId, newFirstReplyCommentId);
        }
        DelayDeleteCommentCacheMqDTO cacheMessage = DelayDeleteCommentCacheMqDTO.builder()
                .commentId(needRemoveChildCommentId).level(CommentLevelEnum.TWO.getCode())
                .noteId(commentDO.getNoteId()).parentId(parentId).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_DELAY_DELETE_CACHE,
                MessageBuilder.withPayload(JsonUtils.toJsonString(cacheMessage)).build(), new SendCallback() {
                    @Override public void onSuccess(SendResult result) { log.info("评论缓存失效消息发送成功"); }
                    @Override public void onException(Throwable error) { log.error("评论缓存失效消息发送失败", error); }
                });
//        重新计算一级评论的热度值
        Set<Long> needSendCommentIds = Sets.newHashSetWithExpectedSize(1);
        needSendCommentIds.add(parentId);

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(needSendCommentIds)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论热度值更新】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论热度值更新】MQ 发送异常: ", throwable);
            }
        });
    }

    private void getReplyCommentIds(List<Long> commentIds, Long needRemoveChildCommentId) {
        List<Long> replyIds = commentDOMapper.selectReplyIdByBeRepliedId(needRemoveChildCommentId);
        commentIds.addAll(replyIds);
        for (Long replyId : replyIds) {
            getReplyCommentIds(commentIds, replyId);
        }
    }
}
