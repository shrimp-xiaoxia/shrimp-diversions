package com.shrimp.data.align.consumer;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.data.align.constants.MQConstants;
import com.shrimp.data.align.constants.RedisKeyConstants;
import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.InsertMapper;
import com.shrimp.data.align.model.dto.CountFollowUnfollowMQDTO;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * @author ccb
 */
@Component
@RocketMQMessageListener(consumerGroup = "shrimp_group_data_align" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
@Slf4j
@RefreshScope
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;

    @Value("${table.shards}")
    private int tableShards;

    public static final DefaultRedisScript<Long> BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT;

    static {
        BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_user_follow_check.lua"));
        BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT.setResultType(Long.class);
    }

    // Lua 脚本：添加到布隆过滤器
    public static final RedisScript<Long> BLOOM_ADD_SCRIPT = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    @Override
    public void onMessage(String message) {
        log.info("## TodayUserFollowIncrementData2DBConsumer 消费到了 MQ: {}", message);

        CountFollowUnfollowMQDTO countFollowUnfollowMQDTO = JsonUtils.parseJSONString(message, CountFollowUnfollowMQDTO.class);
        if (ObjUtil.isNull(countFollowUnfollowMQDTO)) {
            return;
        }
        Long userId = countFollowUnfollowMQDTO.getUserId();
        Long targetUserId = countFollowUnfollowMQDTO.getTargetUserId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // ------------------------- 用户的关注数变更记录 -------------------------
        String bloomFollowingKey = RedisKeyConstants.buildBloomUserRelationFollowingUserIdListKey(date);
        Long luaResult = redisTemplate.execute(BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT, Collections.singletonList(bloomFollowingKey), userId);
        if (ObjUtil.equals(luaResult, 0L)) {
            long hasKey = userId % tableShards;
            String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);

            try {
                insertMapper.insert2DataAlignFollowingCountTempTable(tableNameSuffix, userId);
                redisTemplate.execute(BLOOM_ADD_SCRIPT, Collections.singletonList(bloomFollowingKey), userId);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        // ------------------------- 用户的关注数变更记录 -------------------------
        String bloomFansKey = RedisKeyConstants.buildBloomUserRelationFansUserIdListKey(date);
        luaResult = redisTemplate.execute(BLOOM_TODY_USER_FOLLOW_CHECK_SCRIPT, Collections.singletonList(bloomFansKey), targetUserId);
        if (ObjUtil.equals(luaResult, 0L)) {
            long hasKey = targetUserId % tableShards;
            String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);

            try {
                insertMapper.insert2DataAlignFansCountTempTable(tableNameSuffix, targetUserId);
                redisTemplate.execute(BLOOM_ADD_SCRIPT, Collections.singletonList(bloomFansKey), targetUserId);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
