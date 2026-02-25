package com.shrimp.data.align.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class FollowingCountShardingXxlJob {
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

    @XxlJob("followingCountShardingJobHandler")
    public void followingCountShardingJobHandler() {
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
            //1. 分批次查询 t_data_align_following_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
            List<Long> userIds = selectMapper.selectBatchFromDataAlignFollowingCountTempTable(tableNameSuffix, batchSize);
            if (CollUtil.isEmpty(userIds)) {
                break;
            }
            //2: 循环这一批发生变更的用户 ID， 对 t_following 关注表执行 count(*) 操作，获取总数
            for (Long userId : userIds) {
                //3: 更新 t_user_count 表，并更新对应 Redis 缓存
                int totalCount = selectMapper.selectCountFromFollowingTableByUserId(userId);
                if (totalCount == 0) {
                    continue;
                }

                updateMapper.updateUserFollowingTotalByUserId(userId, totalCount);
                String countUserKey = RedisKeyConstants.buildCountUserKey(userId);
                Boolean hasKey = redisTemplate.hasKey(countUserKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    redisTemplate.opsForHash().put(countUserKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, totalCount);
                }
                // 远程 RPC, 调用搜索服务，重新构建索引
                searchRpcService.rebuildUserDocument(userId);
            }
            //4. 批量物理删除这一批次记录，这思路可以可以！！！
            deleteMapper.batchDeleteDataAlignFollowingCountTempTable(tableNameSuffix, userIds);
            processedTotal += userIds.size();
        }
        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的用户关注数进行对齐，共对齐记录数：{}", processedTotal);
    }
}
