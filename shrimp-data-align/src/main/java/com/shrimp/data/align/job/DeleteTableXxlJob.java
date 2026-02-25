package com.shrimp.data.align.job;

import cn.hutool.core.date.DateTime;
import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.DeleteTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author ccb
 */
@Component
@RefreshScope
public class DeleteTableXxlJob {

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private DeleteTableMapper deleteTableMapper;

    @XxlJob("deleteTableJobHandler")
    public void deleteTableXxlJobHandler() throws Exception {
        XxlJobHelper.log("## 开始删除最近一个月的日增量临时表");
        LocalDate today = LocalDate.now();
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDay = today;
        // 从昨天开始往前推一个月
        LocalDate endDate = today.minusMonths(1);

        while(startDay.isAfter(endDate)){
            startDay = startDay.minusDays(1);
            String date = startDay.format(pattern);
            for(int hasKey = 0;hasKey < tableShards;hasKey++){
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hasKey);
                XxlJobHelper.log("删除表后缀: {}", tableNameSuffix);

                // 删除表
                deleteTableMapper.deleteDataAlignFollowingCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignFansCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 结束删除最近一个月的日增量临时表");
    }
}
