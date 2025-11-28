package com.shrimp.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.shrimp.count.biz.constants.MQConstants;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_" + MQConstants.TOPIC_COUNT_FANS_2_DB,
        topic = MQConstants.TOPIC_COUNT_FANS_2_DB
)
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {
    // 每秒创建 5000 个令牌
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(String message) {
        rateLimiter.acquire();
        log.info("## 消费到了 MQ 【计数: 粉丝数入库】, {}...", message);
        Map<Long, Integer> map = new HashMap<>();
        try {
            map = JsonUtils.parseMap(message, Long.class, Integer.class);
        } catch (Exception e) {
//            TODO 如何处理格式错误的信息
            log.error("## 解析 JSON 字符串异常", e);
        }

        if(CollUtil.isEmpty(map))
            return;
        map.forEach((k,v)->{
            userCountDOMapper.insertOrUpdateFansTotalByUserId(v,k);
        });
    }
}
