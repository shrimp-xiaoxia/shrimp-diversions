package com.shrimp.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.bo.CommentBO;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.model.dto.CountPublishCommentMqDTO;
import com.shrimp.comment.biz.model.dto.PublishCommentMqDTO;
import com.shrimp.comment.biz.rpc.KeyValueRpcService;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Comment2DBConsumer {
    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<Object, String> redisTemplate;

    private DefaultMQPushConsumer consumer;

    private RateLimiter rateLimiter = RateLimiter.create(1000);


    private static final DefaultRedisScript<Long> ADD_HOT_COMMENT_SCRIPT;

    static {
        ADD_HOT_COMMENT_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/add_hot_comments.lua"));
        ADD_HOT_COMMENT_SCRIPT.setScriptSource(resourceScriptSource);
        ADD_HOT_COMMENT_SCRIPT.setResultType(Long.class);
    }


    @Bean
    public DefaultMQPushConsumer mqPushConsumer(ThreadPoolTaskExecutor applicationTaskExecutor) throws MQClientException {
        String group = "shrimp_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

        consumer = new DefaultMQPushConsumer(group);

        consumer.setNamesrvAddr(namesrvAddr);

        consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        consumer.setMessageModel(MessageModel.CLUSTERING);

        consumer.setConsumeMessageBatchMaxSize(30);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            log.info("本次消息拉取的数量：{}", msgs.size());

            try {
                rateLimiter.acquire();
//                转换成MqDTO
                List<PublishCommentMqDTO> publishCommentMqDTOS = Lists.newArrayList();
                msgs.forEach(msg -> {
                    String msgJson = new String(msg.getBody());
                    log.info("==> Consumer - Received message: {}", msgJson);
                    publishCommentMqDTOS.add(JsonUtils.parseJSONString(msgJson, PublishCommentMqDTO.class));
                });
//                清洗数据
                List<CommentBO> commentBOS = getCommentBOS(publishCommentMqDTOS);
//                批量插入
                batchInserDB(commentBOS);

                // 同步一级评论到 Redis 热点评论 ZSET 中
                syncOneLevelComment2RedisZSet(commentBOS);

//              发送MQ消息
                asyncSendMQTOCount(commentBOS);

                // 手动 ACK，告诉 RocketMQ 这批次消息消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("", e);
                // 手动 ACK，告诉 RocketMQ 这批次消息处理失败，稍后再进行重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
        // 启动消费者
        consumer.start();
        return consumer;
    }

    private void syncOneLevelComment2RedisZSet(List<CommentBO> commentBOS) {
//        根据noteId来划分
        Map<Long, List<CommentBO>> mapByNoteId = commentBOS.stream()
                .filter(bo -> ObjUtil.equals(bo.getLevel(), CommentLevelEnum.ONE.getCode()))
                .collect(Collectors.groupingBy(CommentBO::getNoteId));
        if (CollUtil.isEmpty(mapByNoteId)) {
            return;
        }
//       遍历map的元组
        mapByNoteId.entrySet().forEach(entry -> {
//            构建RedisKey
            Long noteId = entry.getKey();
            String commentListKey = RedisKeyConstants.buildCommentListKey(noteId);
//            构建参数列表
            List<CommentBO> commentBOs = entry.getValue();
            Object[] args = new Object[commentBOs.size() * 2];
            for (int i = 0; i < commentBOs.size(); ) {
                args[i] = commentBOs.get(i).getId();
                args[i + 1] = 0;
                i = i + 2;
            }
//            执行Redis命令
            redisTemplate.execute(ADD_HOT_COMMENT_SCRIPT, Collections.singletonList(commentListKey), args);
        });
    }

    private void asyncSendMQTOCount(List<CommentBO> commentBOS) {
        List<CountPublishCommentMqDTO> countPublishCommentMqDTOS = commentBOS.stream().map(commentBO -> CountPublishCommentMqDTO
                .builder()
                .noteId(commentBO.getNoteId())
                .commentId(commentBO.getId())
                .parentId(commentBO.getParentId())
                .level(commentBO.getLevel())
                .build()
        ).toList();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countPublishCommentMqDTOS)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COMMENT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 评论发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 评论发布】MQ 发送异常: ", throwable);
            }
        });
    }

    private List<CommentBO> getCommentBOS(List<PublishCommentMqDTO> publishCommentMqDTOS) {
        //                提取其中的非空replyCommentId
        List<Long> replyCommentIdList = publishCommentMqDTOS.stream()
                .filter(dto -> ObjUtil.isNotNull(dto.getReplyCommentId()))
                .map(PublishCommentMqDTO::getReplyCommentId)
                .toList();
//                根据replyCommentIds查询DB对应的信息
        List<CommentDO> replyCommentDOs = Lists.newArrayList();
        if (CollUtil.isNotEmpty(replyCommentIdList)) {
            replyCommentDOs = commentDOMapper.selectByCommentIds(replyCommentIdList);
        }
//                转换成对应的map
        Map<Long, CommentDO> commentIdAndCommentDOMap = Maps.newHashMap();
        if (CollUtil.isNotEmpty(replyCommentDOs)) {
            commentIdAndCommentDOMap = replyCommentDOs.stream()
                    .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
        }

//                将DTO转换为BO
        List<CommentBO> commentBOS = Lists.newArrayList();
        for (PublishCommentMqDTO publishCommentMqDTO : publishCommentMqDTOS) {
            String imageUrl = publishCommentMqDTO.getImageUrl();
//                    默认没有评论内容，和为一级评论
            CommentBO commentBO = CommentBO.builder()
                    .id(publishCommentMqDTO.getCommentId())
                    .isContentEmpty(true)
                    .level(CommentLevelEnum.ONE.getCode())
                    .imageUrl(imageUrl)
                    .noteId(publishCommentMqDTO.getNoteId())
                    .parentId(publishCommentMqDTO.getNoteId())
                    .isTop(false)
                    .replyCommentId(0L)
                    .userId(publishCommentMqDTO.getCreatorId())
                    .replyTotal(0L)
                    .likeTotal(0L)
                    .replyUserId(0L)
                    .updateTime(publishCommentMqDTO.getCreateTime())
                    .createTime(publishCommentMqDTO.getCreateTime())
                    .build();

//                    处理content
            if (StrUtil.isNotBlank(publishCommentMqDTO.getContent())) {
                // 生成评论内容的 UUID 标识
                commentBO.setContentUuid(UUID.randomUUID().toString());
                commentBO.setIsContentEmpty(false);
                commentBO.setContent(publishCommentMqDTO.getContent());
            }
//                 处理评论级别
            Long replyCommentId = publishCommentMqDTO.getReplyCommentId();
            if (ObjUtil.isNotNull(replyCommentId)) {
//                        是否为一级评论1
                CommentDO commentDO = commentIdAndCommentDOMap.get(replyCommentId);
                if (ObjUtil.isNotNull(commentDO)) {
//                            为二级评论
                    commentBO.setLevel(CommentLevelEnum.TWO.getCode());
                    commentBO.setReplyCommentId(replyCommentId);
                    commentBO.setReplyUserId(commentDO.getUserId());
//                            处理父评论
                    commentBO.setParentId(commentDO.getId());
                    if (ObjUtil.equals(commentDO.getLevel(), CommentLevelEnum.TWO.getCode())) {
                        commentBO.setParentId(commentDO.getParentId());
                    }
                }
            }
            commentBOS.add(commentBO);
        }
        log.info("## 清洗后的 CommentBOS: {}", JsonUtils.toJsonString(commentBOS));
        return commentBOS;
    }

    private void batchInserDB(List<CommentBO> commentBOS) {
        //                开启事务
        transactionTemplate.execute(status -> {
            try {
                commentDOMapper.batchInsert(commentBOS);
                List<CommentBO> batchInsertContent = commentBOS.stream()
                        .filter(commentBO -> Boolean.FALSE.equals(commentBO.getIsContentEmpty()))
                        .toList();
                if (CollUtil.isNotEmpty(batchInsertContent)) {
                    keyValueRpcService.batchSaveCommentContent(batchInsertContent);
                }
                return true;
            } catch (Exception e) {
//                        标记事务回滚
                status.setRollbackOnly();
                log.error("批量插入评论数据事务错误，回滚");
                throw e;
            }
        });
    }

    @PreDestroy
    public void destroy() {
        if (ObjUtil.isNotNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

}
