package com.shrimp.note.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.domain.dataobject.NoteLikeDO;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.enums.LikeUnlikeTypeEnum;
import com.shrimp.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.shrimp.note.biz.rpc.KeyValueRpcService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Component
//@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
//        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
//        consumeMode = ConsumeMode.ORDERLY
//)
//@Slf4j
//public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {
//    @Resource
//    private NoteLikeDOMapper noteLikeDOMapper;
//    @Resource
//    private RocketMQTemplate rocketMQTemplate;
//
//    // 每秒创建 5000 个令牌 TODO 该方法无法动态变化
//    private RateLimiter rateLimiter = RateLimiter.create(5000);
//
//    @Override
//    public void onMessage(Message message) {
//        rateLimiter.acquire();
//        if (ObjUtil.isNull(message)) {
//            return;
//        }
//        String tags = message.getTags();
//        String bodyStr = new String(message.getBody());
//
//        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyStr, tags);
//
//        if (MQConstants.TAG_LIKE.equals(tags)) {
//            handleLikeNoteTagMessage(bodyStr);
//        } else if (MQConstants.TAG_UNLIKE.equals(tags)) {
//            handleUnlikeNoteTagMessage(bodyStr);
//        }
//    }
//
//    private void handleUnlikeNoteTagMessage(String bodyStr) {
//        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseJSONString(bodyStr, LikeUnlikeNoteMqDTO.class);
//        if (ObjUtil.isNull(likeUnlikeNoteMqDTO)) {
//            return;
//        }
/// /        数据库落库
//        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
//                .userId(likeUnlikeNoteMqDTO.getUserId())
//                .noteId(likeUnlikeNoteMqDTO.getNoteId())
//                .status(Byte.valueOf(LikeUnlikeTypeEnum.UNLIKE.getCode().toString()))
//                .createTime(likeUnlikeNoteMqDTO.getCreateTime())
//                .build();
//        int count = noteLikeDOMapper.updateUnlikeNoteByUserIdAndNoteId(noteLikeDO);
//        if (count == 0) return;
/// /       发送MQ到count服务
//        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyStr).build();
//        String topicCountNoteLike = MQConstants.TOPIC_COUNT_NOTE_LIKE;
//
//        rocketMQTemplate.asyncSend(topicCountNoteLike, message, new SendCallback() {
//            @Override
//            public void onSuccess(SendResult sendResult) {
//                log.info("==> 【计数: 笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
//            }
//
//            @Override
//            public void onException(Throwable throwable) {
//                log.error("==> 【计数: 笔记取消点赞】MQ 发送异常: ", throwable);
//            }
//        });
//
//    }
//
//    private void handleLikeNoteTagMessage(String bodyStr) {
/// /        装换数据
//        LikeUnlikeNoteMqDTO likeNoteMqDTO = JsonUtils.parseJSONString(bodyStr, LikeUnlikeNoteMqDTO.class);
//        if (ObjUtil.isNull(likeNoteMqDTO)) {
//            return;
//        }
/// /        数据库落库
//        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
//                .userId(likeNoteMqDTO.getUserId())
//                .noteId(likeNoteMqDTO.getNoteId())
//                .createTime(likeNoteMqDTO.getCreateTime())
//                .status(Byte.valueOf(LikeUnlikeTypeEnum.LIKE.getCode().toString()))
//                .build();
//        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);
//        if (count == 0) return;

/// /         发送MQ到count
//        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyStr).build();
//        String topicCountNoteLike = MQConstants.TOPIC_COUNT_NOTE_LIKE;
//
//        rocketMQTemplate.asyncSend(topicCountNoteLike, message, new SendCallback() {
//            @Override
//            public void onSuccess(SendResult sendResult) {
//                log.info("==> 【计数: 笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
//            }
//
//            @Override
//            public void onException(Throwable throwable) {
//                log.error("==> 【计数: 笔记点赞】MQ 发送异常: ", throwable);
//            }
//        });
//
//    }
//}


@Component
@Slf4j
public class LikeUnlikeNoteConsumer {
    @Value("${rocketmq.name-server}")
    private String namesrvAddr;
    @Resource
    private TransactionTemplate transactionTemplate;

    private DefaultMQPushConsumer consumer;

    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Bean(name = "LikeUnlikeNoteConsumer")
    public DefaultMQPushConsumer mqPushConsumer(NoteLikeDOMapper noteLikeDOMapper) throws MQClientException {
        String group = "shrimp_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE;

        consumer = new DefaultMQPushConsumer(group);

        consumer.setNamesrvAddr(namesrvAddr);

        consumer.subscribe(MQConstants.TOPIC_LIKE_OR_UNLIKE, "*");

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        consumer.setMessageModel(MessageModel.BROADCASTING);
        // 最大重试次数, 以防消息重试过多次仍然没有成功，避免消息卡在消费队列中。
        consumer.setMaxReconsumeTimes(3);
        // 设置每批次消费的最大消息数量，这里设置为 30，表示每次拉取时最多消费 30 条消息。
        consumer.setConsumeMessageBatchMaxSize(30);
        // 设置拉取间隔, 单位毫秒
        consumer.setPullInterval(1000);
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【笔记点赞、取消点赞】本批次消息大小: {}", msgs.size());
            try {
//              令牌限流
                rateLimiter.acquire();
                // TODO 幂等性: 通过联合唯一索引保证

                // 消息体 Json 字符串转 DTO
                List<LikeUnlikeNoteMqDTO> likeUnlikeNoteMqDTOS = Lists.newArrayList();
                msgs.forEach(msg -> {
                    String msgJson = new String(msg.getBody());
                    log.info("==> Consumer - Received message: {}", msgJson);
                    LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseJSONString(msgJson, LikeUnlikeNoteMqDTO.class);
                    likeUnlikeNoteMqDTOS.add(likeUnlikeNoteMqDTO);
                });
                // 1. 内存级操作合并
//                先按用户ID分组
                Map<Long, List<LikeUnlikeNoteMqDTO>> mapByUserId = likeUnlikeNoteMqDTOS.stream()
                        .collect(Collectors.groupingBy(LikeUnlikeNoteMqDTO::getUserId));
//                再按noteId分组
                List<LikeUnlikeNoteMqDTO> finalOperation = Lists.newArrayList();
                mapByUserId.values().forEach(list -> {
                    Map<Long, List<LikeUnlikeNoteMqDTO>> mapByUserIdAndNoteId = list.stream().
                            collect(Collectors.groupingBy(LikeUnlikeNoteMqDTO::getNoteId));
//                找出奇数操作的消息
                    List<LikeUnlikeNoteMqDTO> finalOperationByUserId = mapByUserIdAndNoteId.values().stream().filter(
                                    value -> {
                                        int size = value.size();
                                        if (size % 2 == 0) {
                                            return false;
                                        }
                                        return true;
                                    }
                            ).map(value -> value.get(value.size() - 1))
                            .toList();
                    finalOperation.addAll(finalOperationByUserId);
                });

                // 2. 批量写入数据库
                if (CollUtil.isNotEmpty(finalOperation)) {
                    List<NoteLikeDO> noteLikeDOS = finalOperation.stream()
                            .map(dto -> NoteLikeDO.builder()
                                    .noteId(dto.getNoteId())
                                    .userId(dto.getUserId())
                                    .status(Byte.valueOf(dto.getType().toString()))
                                    .createTime(dto.getCreateTime())
                                    .build()).toList();

                    noteLikeDOMapper.batchInsertOrUpdate(noteLikeDOS);
                }

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

    @PreDestroy
    public void destroy() {
        if (ObjUtil.isNotNull(consumer)) {
            try {
//              关闭消费者
                consumer.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
