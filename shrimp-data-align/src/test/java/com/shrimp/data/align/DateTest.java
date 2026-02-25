package com.shrimp.data.align;

import com.shrimp.data.align.constants.TableConstants;
import com.shrimp.data.align.domain.mapper.CreateTableMapper;
import com.xxl.job.core.handler.IJobHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SpringBootTest
public class DateTest {

    @Resource
    private CreateTableMapper createTableMapper;

    @Test
    public void createTable() throws Exception {
        String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, 1);
        createTableMapper.createDataAlignNotePublishCountTempTable(tableNameSuffix);
    }

}
