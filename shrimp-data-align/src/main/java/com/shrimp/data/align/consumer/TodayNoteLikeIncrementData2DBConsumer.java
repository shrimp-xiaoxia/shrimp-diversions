package com.shrimp.data.align.consumer;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.data.align.constants.MQConstants;
import com.shrimp.data.align.constants.RedisKeyConstants;
import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.InsertMapper;
import com.shrimp.data.align.model.dto.LikeUnlikeNoteMqDTO;
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
@RocketMQMessageListener(consumerGroup = "shrimp_group_data_align" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
@Slf4j
@RefreshScope
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;
    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    public static final DefaultRedisScript<Long> BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT;

    static {
        BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_like_check.lua"));
        BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT.setResultType(Long.class);
    }

    // Lua 脚本：添加到布隆过滤器
    public static final RedisScript<Long> BLOOM_ADD_SCRIPT = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    @Override
    public void onMessage(String message) {
        log.info("## TodayNoteLikeIncrementData2DBConsumer 消费到了 MQ: {}", message);
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseJSONString(message, LikeUnlikeNoteMqDTO.class);
        if (ObjUtil.isNull(likeUnlikeNoteMqDTO)) {
            return;
        }
        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        Long noteCreatorId = likeUnlikeNoteMqDTO.getNoteCreatorId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ------------------------- 笔记的点赞数变更记录 -------------------------
        // 笔记对应的 Bloom Key
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteLikeNoteIdListKey(date);
        //1. 布隆过滤器判断该日增量数据是否已经记录
        Long luaResult = redisTemplate.execute(BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT, Collections.singletonList(noteBloomKey), noteId);
        if (ObjUtil.equals(luaResult, 0L)) {
            //2. 若无，才会落库，减轻数据库压力
            long hasKey = noteId % tableShards;
            String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);
            // - t_data_align_note_like_count_temp_日期_分片序号
            try {
                insertMapper.insert2DataAlignNoteLikeCountTempTable(tableNameSuffix, noteId);
                // 3. 数据库写入成功后，再添加布隆过滤器中
                redisTemplate.execute(BLOOM_ADD_SCRIPT, Collections.singletonList(noteBloomKey), noteId);
            } catch (Exception e) {
                log.error("", e);
            }
        }

        // ------------------------- 笔记发布者获得的点赞数变更记录 -------------------------
        // 笔记发布者对应的 Bloom Key
        String userBloomKey = RedisKeyConstants.buildBloomUserNoteLikeUserIdListKey(date);
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        luaResult = redisTemplate.execute(BLOOM_TODY_NOTE_LIKE_CHECK_SCRIPT, Collections.singletonList(userBloomKey), noteCreatorId);
        // 2. 若无，才会落库，减轻数据库压力
        if (ObjUtil.equals(luaResult, 0L)) {
            long hasKey = noteCreatorId % tableShards;
            // t_data_align_user_like_count_temp_日期_分片序号
            String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);
            // 3. 数据库写入成功后，再添加布隆过滤器中
            try {
                insertMapper.insert2DataAlignUserLikeCountTempTable(tableNameSuffix, noteCreatorId);
                redisTemplate.execute(BLOOM_ADD_SCRIPT, Collections.singletonList(userBloomKey), noteCreatorId);
            } catch (Exception e) {
                log.error("", e);
            }
        }

    }
}
