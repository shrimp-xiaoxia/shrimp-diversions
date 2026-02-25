package com.shrimp.data.align.consumer;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.data.align.constants.MQConstants;
import com.shrimp.data.align.constants.RedisKeyConstants;
import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.InsertMapper;
import com.shrimp.data.align.model.dto.OperateNoteMqDTO;
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
@RocketMQMessageListener(consumerGroup = "shrimp_group_data_align" + MQConstants.TOPIC_COUNT_NOTE_OPERATE,
        topic = MQConstants.TOPIC_COUNT_NOTE_OPERATE
)
@Slf4j
@RefreshScope
public class TodayNoteOperateIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;

    @Value("${table.shards}")
    private int tableShards;

    public static final DefaultRedisScript<Long> BLOOM_TODY_NOTE_OPERATE_CHECK_SCRIPT;

    static {
        BLOOM_TODY_NOTE_OPERATE_CHECK_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_operate_check.lua"));
        BLOOM_TODY_NOTE_OPERATE_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_TODY_NOTE_OPERATE_CHECK_SCRIPT.setResultType(Long.class);
    }

    // Lua 脚本：添加到布隆过滤器
    public static final RedisScript<Long> BLOOM_ADD_SCRIPT = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    @Override
    public void onMessage(String message) {
        log.info("## TodayNoteOperateIncrementData2DBConsumer 消费到了 MQ: {}", message);
        OperateNoteMqDTO operateNoteMqDTO = JsonUtils.parseJSONString(message, OperateNoteMqDTO.class);
        if (ObjUtil.isNull(operateNoteMqDTO)) {
            return;
        }
        Long creatorId = operateNoteMqDTO.getCreatorId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // ------------------------- 用户的发布笔记数变更记录 -------------------------
        String bloomUserKey = RedisKeyConstants.buildBloomUserNoteOperateUserIdListKey(date);
        Long luaResult = redisTemplate.execute(BLOOM_TODY_NOTE_OPERATE_CHECK_SCRIPT, Collections.singletonList(bloomUserKey), creatorId);
        if (ObjUtil.equals(luaResult, 0L)) {
            long hasKey = creatorId % tableShards;
            String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);

            try {
                insertMapper.insert2DataAlignUserNotePublishCountTempTable(tableNameSuffix, creatorId);
                redisTemplate.execute(BLOOM_ADD_SCRIPT, Collections.singletonList(bloomUserKey), creatorId);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
