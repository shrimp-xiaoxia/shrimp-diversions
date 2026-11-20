package com.shrimp.user.biz.controller;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.exception.GlobalExceptionHandler;
import com.shrimp.user.biz.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserUpdateContractTest {
    private MockMvc mvc;
    private UserDOMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = mock(UserDOMapper.class);
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userDOMapper", mapper);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        LoginUserContextHolder.setLoginUserId(7001L);
    }

    @AfterEach
    void cleanUp() {
        LoginUserContextHolder.removeLoginUserContext();
    }

    @Test
    void multipartBindsFieldsAndUpdatesLoggedInUser() throws Exception {
        mvc.perform(multipart("/user/update")
                        .param("nickName", "测试昵称").param("introduction", "test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        ArgumentCaptor<UserDO> update = ArgumentCaptor.forClass(UserDO.class);
        verify(mapper).updateByPrimaryKeySelective(update.capture());
        assertEquals(7001L, update.getValue().getId());
        assertEquals("测试昵称", update.getValue().getNickname());
        assertEquals("test", update.getValue().getIntroduction());
    }

    @Test
    void jsonIsRejectedWithExplicitMultipartContract() throws Exception {
        mvc.perform(post("/user/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introduction\":\"test\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USER-20005"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("multipart/form-data")));
        verifyNoInteractions(mapper);
    }

    @Test
    void urlEncodedRequestIsRejectedBeforeAnyDatabaseWrite() throws Exception {
        mvc.perform(post("/user/update").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("introduction", "test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("USER-20005"));
        verifyNoInteractions(mapper);
    }
}
