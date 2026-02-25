package com.shrimp.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.shrimp.data.align.constants.RedisKeyConstants;
import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.DeleteMapper;
import com.shrimp.data.align.domain.mapper.SelectMapper;
import com.shrimp.data.align.domain.mapper.UpdateMapper;
import com.shrimp.data.align.rpc.SearchRpcService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class NoteLikeCountShardingXxlJob {
    @Resource
    private SelectMapper selectMapper;
    @Resource
    private UpdateMapper updateMapper;
    @Resource
    private DeleteMapper deleteMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SearchRpcService searchRpcService;

    @XxlJob("noteLikeCountShardingJobHandler")
    public void noteLikeCountShardingJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);
        int batchSize = 1000;
        // 共对齐了多少条记录，默认为 0
        long processedTotal = 0;

        while (true) {
            List<Long> noteIds = selectMapper.selectBatchFromDataAlignNoteLikeCountTempTable(tableNameSuffix, batchSize);
            if (CollUtil.isEmpty(noteIds)) {
                break;
            }

            for (Long noteId : noteIds) {
                int totalCount = selectMapper.selectCountFromNoteLikeTableByNoteId(noteId);
                if (totalCount == 0) {
                    continue;
                }

                updateMapper.updateNoteLikeTotalByNoteId(noteId, totalCount);
                String countNoteKey = RedisKeyConstants.buildCountNoteKey(noteId);
                Boolean hasKey = redisTemplate.hasKey(countNoteKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    redisTemplate.opsForHash().put(countNoteKey, RedisKeyConstants.FIELD_LIKE_TOTAL, totalCount);
                }
                searchRpcService.rebuildNoteDocument(noteId);
            }
            deleteMapper.batchDeleteDataAlignNoteLikeCountTempTable(tableNameSuffix, noteIds);
            processedTotal += noteIds.size();
        }
        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的笔记点赞数进行对齐，共对齐记录数：{}", processedTotal);
    }
}


