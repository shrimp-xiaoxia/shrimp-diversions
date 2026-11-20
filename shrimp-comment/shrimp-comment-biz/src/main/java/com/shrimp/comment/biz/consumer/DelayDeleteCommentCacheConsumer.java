package com.shrimp.comment.biz.consumer;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.enums.ResponseCodeEnum;
import com.shrimp.comment.biz.model.dto.DelayDeleteCommentCacheMqDTO;
import com.shrimp.comment.biz.service.CommentService;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COMMENT_DELAY_DELETE_CACHE, // Group 组
        topic = MQConstants.TOPIC_COMMENT_DELAY_DELETE_CACHE, // 主题 Topic
        messageModel = MessageModel.BROADCASTING
)
@Slf4j
public class DelayDeleteCommentCacheConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private CommentService commentService;

    @Override
    public void onMessage(String body) {
        log.info("==>[删除评论缓存]，sendBody:{}", body);
        DelayDeleteCommentCacheMqDTO mqDTO = null;
        try {
            mqDTO = JsonUtils.parseJSONString(body, DelayDeleteCommentCacheMqDTO.class);
        } catch (Exception e) {
            log.error("解析Json字符串失败");
            return;
        }
        Long commentId = mqDTO.getCommentId();
        Integer level = mqDTO.getLevel();

//        构建zset Key 和 STRING的详情缓存KEY
        String listIdKey = null;
        String commentDetailKey = null;
        if (ObjUtil.equals(level, CommentLevelEnum.ONE.getCode())) {
            listIdKey = RedisKeyConstants.buildCommentListKey(mqDTO.getNoteId());
            commentDetailKey = RedisKeyConstants.buildCommentDetailKey(commentId);
        } else if (ObjUtil.equals(level, CommentLevelEnum.TWO.getCode())) {
            listIdKey = RedisKeyConstants.buildChildCommentListKey(mqDTO.getParentId());
            commentDetailKey = RedisKeyConstants.buildChildCommentDetailKey(commentId);
        } else {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        redisTemplate.opsForZSet().remove(listIdKey, commentId);
//        删除STRING的详情缓存（Redis和本地）
        redisTemplate.delete(commentDetailKey);
//        幂等操作
        if (ObjUtil.equals(level, CommentLevelEnum.ONE.getCode())) {
            commentService.removeCommentLocalCache(commentId);
        } else {
            redisTemplate.delete(RedisKeyConstants.buildCommentDetailKey(mqDTO.getParentId()));
            commentService.removeCommentLocalCache(mqDTO.getParentId());
        }
    }
}
