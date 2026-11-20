package com.shrimp.count.biz.controller;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.InputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "SHRIMP_LIVE_DB_TEST", matches = "true")
class LiveCountMapperTest {
    @Test void readsCountTablesWithoutWriting() throws Exception {
        var dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", System.getenv("SHRIMP_TEST_MYSQL_URL"), "root", System.getenv("SHRIMP_TEST_MYSQL_PASSWORD"));
        var config = new Configuration(new Environment("readonly-check", new JdbcTransactionFactory(), dataSource));
        for (String name : new String[]{"NoteCountDOMapper", "UserCountDOMapper"}) {
            String resource = "mapper/" + name + ".xml";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream); new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            session.getConnection().setReadOnly(true);
            assertNotNull(session.getMapper(NoteCountDOMapper.class).selectByNoteIds(List.of(0L)));
            assertNotNull(session.getMapper(UserCountDOMapper.class).selectByUserIds(List.of(0L)));
        }
    }
}
