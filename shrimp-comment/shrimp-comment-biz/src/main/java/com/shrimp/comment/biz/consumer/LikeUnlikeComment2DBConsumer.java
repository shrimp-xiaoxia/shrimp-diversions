package com.shrimp.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.shrimp.comment.biz.enums.LikeUnlikeCommentTypeEnum;
import com.shrimp.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LikeUnlikeComment2DBConsumer {
    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    private DefaultMQPushConsumer consumer;

    @Resource
    private CommentLikeDOMapper commentLikeDOMapper;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Bean(name = "LikeUnLikeComment2DBConsumer")
    public DefaultMQPushConsumer mqPushConsumer() throws MQClientException {
//        Group组
        String group = "shrimp_group" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE;
//      创建DefaultMQPushConsumer实例,并指定消费者的消费组名
        consumer = new DefaultMQPushConsumer(group);
//        设置RocketMQ的nameServer地址
        consumer.setNamesrvAddr(namesrvAddr);
//        订阅主题
        consumer.subscribe(MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE, "*");
//      设置消费者消费消息的起始位置
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
//        设置消息消费模式，这里是使用集群模式
        consumer.setMessageModel(MessageModel.CLUSTERING);
//        设置拉取消息的数量
        consumer.setConsumeMessageBatchMaxSize(30);
//        设置消费最多重试三次
        consumer.setMaxReconsumeTimes(3);
//        注册消息监听器
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【评论点赞、取消点赞】本批次消息大小: {}", msgs.size());
            try {
                rateLimiter.acquire();
//                设置消息DTO
                List<LikeUnlikeCommentMqDTO> likeUnlikeCommentMqDTOList = Lists.newArrayList();
                msgs.forEach(msg -> {
                    String tag = msg.getTags(); // Tag 标签
                    String msgJson = new String(msg.getBody()); // 消息体 Json 字符串
                    log.info("==> 【评论点赞、取消点赞】Consumer - Tag: {}, Received message: {}", tag, msgJson);

                    likeUnlikeCommentMqDTOList.add(JsonUtils.parseJSONString(msgJson, LikeUnlikeCommentMqDTO.class));
                });
//                转化为idAndListMap
                Map<Long, List<LikeUnlikeCommentMqDTO>> commentIdAndDTOListMap = likeUnlikeCommentMqDTOList.stream().collect(Collectors.groupingBy(LikeUnlikeCommentMqDTO::getCommentId));
//                设置最终等价的消息体DTO
                List<LikeUnlikeCommentMqDTO> finalUserOpsList = Lists.newArrayList();
//                遍历idAndListMap
                for (Map.Entry<Long, List<LikeUnlikeCommentMqDTO>> entry : commentIdAndDTOListMap.entrySet()) {
                    // 优化：若某个用户对某评论，多次操作，如点赞 -> 取消点赞 -> 点赞，需进行操作合并，只提取最后一次操作，进一步降低操作数据库的频率
                    Long commentId = entry.getKey();
                    List<LikeUnlikeCommentMqDTO> commentMqDTOList = entry.getValue();
                    Map<Long, LikeUnlikeCommentMqDTO> userLastOps = commentMqDTOList.stream().collect(Collectors.toMap(
                            LikeUnlikeCommentMqDTO::getUserId,
                            Function.identity(),
//                            合并策略
                            (first, second) -> first.getCreateTime().isAfter(second.getCreateTime()) ? first : second
                    ));
                    finalUserOpsList.addAll(userLastOps.values());
                }
//                批量消费
                executeBatchDB(finalUserOpsList);
                // 手动 ACK，告诉 RocketMQ 这批次消息消费成功
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("", e);
                // 这样 RocketMQ 会暂停当前队列的消费一段时间，再重试
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });
//        启动消费者
        consumer.start();
        return consumer;
    }

    private void executeBatchDB(List<LikeUnlikeCommentMqDTO> finalUserOpsList) {
//        提取点赞列表
        List<LikeUnlikeCommentMqDTO> likeList = finalUserOpsList.stream()
                .filter(ops -> ObjUtil.equals(LikeUnlikeCommentTypeEnum.LIKE.getCode(), ops.getType()))
                .toList();
//        提取取消点赞列表
        List<LikeUnlikeCommentMqDTO> unlikeList = finalUserOpsList.stream()
                .filter(ops -> ObjUtil.equals(LikeUnlikeCommentTypeEnum.UNLIKE.getCode(), ops.getType()))
                .toList();
//        执行SQL
        if (CollUtil.isNotEmpty(likeList)) {
            commentLikeDOMapper.batchInsert(likeList);
        }
        if (CollUtil.isNotEmpty(unlikeList)) {
            commentLikeDOMapper.batchDelete(unlikeList);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ObjUtil.isNotNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
