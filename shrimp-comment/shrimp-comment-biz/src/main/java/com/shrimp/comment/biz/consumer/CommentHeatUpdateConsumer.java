package com.shrimp.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.bo.CommentBO;
import com.shrimp.comment.biz.domain.bo.CommentHeatBO;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.util.HeatCalculator;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COMMENT_HEAT_UPDATE, // Group 组
        topic = MQConstants.TOPIC_COMMENT_HEAT_UPDATE // 主题 Topic
)
@Slf4j
public class CommentHeatUpdateConsumer implements RocketMQListener<String> {

    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private RedisTemplate<Object, String> redisTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(300)   // 一批次最多聚合 300 条
            .linger(Duration.ofSeconds(2)) // 多久聚合一次（2s 一次）
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    private static final DefaultRedisScript<Long> UPDATE_HOT_COMMENTS_SCRIPT;

    static {
        UPDATE_HOT_COMMENTS_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/update_hot_comments.lua"));
        UPDATE_HOT_COMMENTS_SCRIPT.setScriptSource(resourceScriptSource);
        UPDATE_HOT_COMMENTS_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【评论热度值计算】聚合消息, size: {}", bodys.size());
        log.info("==> 【评论热度值计算】聚合消息, {}", JsonUtils.toJsonString(bodys));

        // 将聚合后的消息体 Json 转 Set<Long>, 去重相同的评论 ID, 防止重复计算
        Set<Long> commentIds = Sets.newHashSet();
        bodys.forEach(body -> {
            try {
                Set<Long> list = JsonUtils.parseSet(body, Long.class);
                commentIds.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        log.info("==> 去重后的评论 ID: {}", commentIds);
//      批量查询
        List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(commentIds.stream().toList());
//      遍历集合，获取heat
        List<Long> ids = Lists.newArrayList();
        List<CommentHeatBO> commentHeatBOS = Lists.newArrayList();
//        批量更新
        commentDOS.forEach(commentDO -> {
            Long id = commentDO.getId();
            Long likeTotal = commentDO.getLikeTotal();
            Long childCommentTotal = commentDO.getChildCommentTotal();

            double heat = HeatCalculator.calculateHeat(likeTotal, childCommentTotal).doubleValue();

            ids.add(id);
            commentHeatBOS.add(CommentHeatBO.builder()
                    .id(id)
                    .noteId(commentDO.getNoteId())
                    .heat(heat)
                    .level(commentDO.getLevel())
                    .build());
        });
        commentDOMapper.batchUpdateHeatByCommentIds(ids, commentHeatBOS);

        updateCommentList2Redis(commentHeatBOS);
    }

    private void updateCommentList2Redis(List<CommentHeatBO> commentHeatBOS) {
//        过滤出一级评论
        //        分组
        Map<Long, List<CommentHeatBO>> mapByNoteId = commentHeatBOS.stream()
                .filter(bo -> ObjUtil.equals(bo.getLevel(), CommentLevelEnum.ONE.getValue()))
                .collect(Collectors.groupingBy(CommentHeatBO::getNoteId));

        if (CollUtil.isEmpty(mapByNoteId)) {
            return;
        }
//        遍历
        mapByNoteId.entrySet().forEach(entry -> {
            Long noteId = entry.getKey();
            String commentListKey = RedisKeyConstants.buildCommentListKey(noteId);

//            构建参数
            List<CommentHeatBO> argsList = entry.getValue();
            Object[] args = new Object[argsList.size() * 2];
            for (int i = 0; i < argsList.size(); ) {
                args[i] = argsList.get(i).getId();
                args[i + 1] = argsList.get(i).getHeat();
                i = i + 2;
            }

            redisTemplate.execute(UPDATE_HOT_COMMENTS_SCRIPT, Collections.singletonList(commentListKey), args);
        });
//
    }
}