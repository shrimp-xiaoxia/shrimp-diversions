package com.shrimp.user.relation.biz.controller;
import com.shrimp.user.relation.biz.domain.mapper.FansDOMapper;
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
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
class LiveRelationMapperTest {
    @Test void filtersAndPagesAgainstRealSchema() throws Exception {
        var dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", System.getenv("SHRIMP_TEST_MYSQL_URL"), "root", System.getenv("SHRIMP_TEST_MYSQL_PASSWORD"));
        var config = new Configuration(new Environment("readonly-check", new JdbcTransactionFactory(), dataSource));
        for (String name : new String[]{"FollowingDOMapper", "FansDOMapper"}) {
            String resource = "mapper/" + name + ".xml";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream); new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            session.getConnection().setReadOnly(true);
            FollowingDOMapper following = session.getMapper(FollowingDOMapper.class);
            FansDOMapper fans = session.getMapper(FansDOMapper.class);
            assertTrue(following.countAvailable(0L) >= 0); assertNotNull(following.pageAvailable(0L, 0));
            assertNotNull(following.selectFollowingIds(0L, List.of(0L)));
            assertTrue(fans.countAvailable(0L) >= 0); assertNotNull(fans.pageAvailable(0L, 0));
        }
    }
}
