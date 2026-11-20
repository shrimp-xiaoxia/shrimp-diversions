package com.shrimp.count.biz.consumer;

import cn.hutool.core.util.ObjUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.mapper.CommentDOMapper;
import com.shrimp.count.biz.model.dto.AggregationCountLikeUnlikeCommentMqDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_count_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE_2_DB,
        topic = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE_2_DB
)
@Slf4j
public class CountLikeUnlikeComment2DBConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private RateLimiter rateLimiter;

    @Override
    public void onMessage(String body) {
        rateLimiter.acquire();
        log.info("==>[评论计数落库] sendBody:{}", body);
//        转化
        AggregationCountLikeUnlikeCommentMqDTO aggMqDTO = null;
        try {
            aggMqDTO = JsonUtils.parseJSONString(body, AggregationCountLikeUnlikeCommentMqDTO.class);
        } catch (Exception e) {
            log.error("解析Json字符串失败");
        }
//        落库
        if (ObjUtil.isNull(aggMqDTO)) {
            return;
        }
        commentDOMapper.updateCommentLikeTotal(aggMqDTO.getCommentId(), aggMqDTO.getCount());
    }


}
