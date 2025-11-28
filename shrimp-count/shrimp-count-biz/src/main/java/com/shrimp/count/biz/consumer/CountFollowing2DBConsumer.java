package com.shrimp.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.enums.FollowUnFollowTypeEnum;
import com.shrimp.count.biz.model.dto.CountFollowUnfollowMQDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB
)
@Slf4j
public class CountFollowing2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(String message) {
        rateLimiter.acquire();
        log.info("## 消费到了 MQ 【计数: 关注数入库】, {}...", message);
        if (StrUtil.isBlank(message)) {
            return;
        }
        CountFollowUnfollowMQDTO mqDto = JsonUtils.parseJSONString(message, CountFollowUnfollowMQDTO.class);

        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(
                FollowUnFollowTypeEnum.FOLLOW.getCode().equals(mqDto.getType()) ? 1 : -1, mqDto.getUserId());
    }
}
