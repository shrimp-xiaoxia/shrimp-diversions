package com.shrimp.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.model.dto.CountPublishCommentMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_first_reply_comment_id" + MQConstants.TOPIC_COUNT_NOTE_COMMENT, // Group 组
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT // 主题 Topic
)
@Slf4j
public class OneLevelCommentFirstReplyCommentIdUpdateConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();
    @Autowired
    private CommentDOMapper commentDOMapper;

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, size: {}", bodys.size());
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, {}", JsonUtils.toJsonString(bodys));

        // 将聚合后的消息体 Json 转 List<CountPublishCommentMqDTO>
        List<CountPublishCommentMqDTO> publishCommentMqDTOS = Lists.newArrayList();
        bodys.forEach(body -> {
            try {
                List<CountPublishCommentMqDTO> list = JsonUtils.parseList(body, CountPublishCommentMqDTO.class);
                publishCommentMqDTOS.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        // 过滤出二级评论的 parent_id（即一级评论 ID），并去重，需要更新对应一级评论的 first_reply_comment_id
        List<Long> parentIds = publishCommentMqDTOS.stream()
                .filter(publishCommentMqDTO -> Objects.equals(publishCommentMqDTO.getLevel(), CommentLevelEnum.TWO.getValue()))
                .map(CountPublishCommentMqDTO::getParentId)
                .distinct() // 去重
                .toList();

        if (CollUtil.isEmpty(parentIds)) return;

        List<String> redisKeys = parentIds.stream()
                .map(RedisKeyConstants::buildHaveFirstReplyCommentKey)
                .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);

        List<Long> missingCommentIds = Lists.newArrayList();
        for (int i = 0; i < values.size(); i++) {
            if (ObjUtil.isNull(values.get(i))) {
                missingCommentIds.add(parentIds.get(i));
            }
        }

        if (CollUtil.isEmpty(missingCommentIds))
            return;

        // 存在的一级评论 ID，说明表中对应记录的 first_reply_comment_id 已经有值
        // 不存在的，则需要进一步查询数据库来确定，是否要更新记录对应的 first_reply_comment_id 值
        List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(missingCommentIds);
        threadPoolTaskExecutor.submit(() -> {
            List<Long> needPushTORedisIds = commentDOS.stream()
                    .filter(commentDO -> commentDO.getFirstReplyCommentId() != 0)
                    .map(CommentDO::getId)
                    .toList();
            sync2Redis(needPushTORedisIds);
        });

        // 过滤出值为 0 的，都需要更新其 first_reply_comment_id
        List<CommentDO> needUpdateCommentDOS = commentDOS.stream()
                .filter(commentDO -> commentDO.getFirstReplyCommentId() == 0)
                .toList();

//      批量查询出每一条需要修改第一回复评论ID的数据
        needUpdateCommentDOS = commentDOMapper.batchSelectEarliestByParentIds(needUpdateCommentDOS.stream()
                .map(CommentDO::getId).toList());

        if(CollUtil.isEmpty(needUpdateCommentDOS)){
            return;
        }

//    批量修改
        List<Long> ids = needUpdateCommentDOS.stream()
                .map(CommentDO::getParentId)
                .toList();
        commentDOMapper.batchUpdateFirstReplyCommentIdByPrimaryKey(needUpdateCommentDOS, ids);
//   批量同步到redis
        threadPoolTaskExecutor.submit(() -> sync2Redis(ids));
    }


    private void sync2Redis(List<Long> needSyncCommentIds) {
        List<String> redisKeys = needSyncCommentIds.stream()
                .map(RedisKeyConstants::buildHaveFirstReplyCommentKey)
                .toList();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long commentId : needSyncCommentIds) {
                String key = RedisKeyConstants.buildHaveFirstReplyCommentKey(commentId);
                int expireSeconds = RandomUtil.randomInt(5 * 60 * 60);

                connection.setEx(
                        key.getBytes(StandardCharsets.UTF_8),
                        expireSeconds,
                        "1".getBytes(StandardCharsets.UTF_8)
                );
            }
            return null;
        });
    }
}
