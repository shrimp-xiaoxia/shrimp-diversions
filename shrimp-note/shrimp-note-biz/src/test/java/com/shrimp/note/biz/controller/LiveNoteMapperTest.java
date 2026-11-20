package com.shrimp.note.biz.controller;

import com.shrimp.note.biz.domain.mapper.ChannelDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.TopicDOMapper;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.framework.common.response.Response;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "SHRIMP_LIVE_DB_TEST", matches = "true")
class LiveNoteMapperTest {
    @Test void readsExistingTablesWithoutChangingData() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
                System.getenv("SHRIMP_TEST_MYSQL_URL"), "root", System.getenv("SHRIMP_TEST_MYSQL_PASSWORD"));
        Configuration config = new Configuration(new Environment("readonly-check", new JdbcTransactionFactory(), dataSource));
        for (String name : new String[]{"ChannelDOMapper", "TopicDOMapper", "NoteDOMapper"}) {
            String resource = "mapper/" + name + ".xml";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
        try (SqlSession session = factory.openSession()) {
            session.getConnection().setReadOnly(true);
            assertNotNull(session.getMapper(ChannelDOMapper.class).selectEnabled());
            assertNotNull(session.getMapper(TopicDOMapper.class).searchEnabled(""));
            NoteDOMapper notes = session.getMapper(NoteDOMapper.class);
            assertTrue(notes.countDiscover(0L) >= 0);
            assertNotNull(notes.findDiscover(0L, 0));
            assertTrue(notes.countProfile(0L, 2, true) >= 0);
            assertNotNull(notes.findProfile(0L, 3, true, 0));

            UserFeignApi users = mock(UserFeignApi.class);
            when(users.findUserByIds(any())).thenAnswer(call -> {
                FindUserByIdsReqDTO request = call.getArgument(0);
                return Response.success(request.getIds().stream()
                        .map(id -> FindUserByIdRspDTO.builder().id(id).nickName("test").build()).toList());
            });
            CountFeignApi counts = mock(CountFeignApi.class);
            when(counts.findNotes(any())).thenAnswer(call -> {
                CountQueryReqDTO request = call.getArgument(0);
                return Response.success(request.getIds().stream().map(id -> {
                    CountResultDTO result = new CountResultDTO(); result.setId(id); result.setLikeTotal(0L); return result;
                }).toList());
            });
            NoteListController controller = new NoteListController();
            ReflectionTestUtils.setField(controller, "notes", notes);
            ReflectionTestUtils.setField(controller, "channels", session.getMapper(ChannelDOMapper.class));
            ReflectionTestUtils.setField(controller, "users", users);
            ReflectionTestUtils.setField(controller, "counts", counts);
            MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(post("/discover/note/list").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"channelId\":0,\"pageNo\":1}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").isString());
        }
    }
}
