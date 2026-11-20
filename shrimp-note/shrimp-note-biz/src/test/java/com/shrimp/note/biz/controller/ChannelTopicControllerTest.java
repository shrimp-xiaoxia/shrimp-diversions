package com.shrimp.note.biz.controller;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.note.biz.domain.dataobject.TopicDO;
import com.shrimp.note.biz.domain.mapper.TopicDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.TopicCreateReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChannelTopicControllerTest {
    @BeforeEach void login() { LoginUserContextHolder.setLoginUserId(1L); }
    @AfterEach void clear() { LoginUserContextHolder.removeLoginUserContext(); }
    @Test void rejectsWhitespaceAndLongNamesBeforeDatabaseWrites() {
        ChannelTopicController controller = new ChannelTopicController();
        TopicDOMapper topics = mock(TopicDOMapper.class);
        ReflectionTestUtils.setField(controller, "topicMapper", topics);
        TopicCreateReqVO request = new TopicCreateReqVO(); request.setName("a b");
        assertEquals(ResponseCodeEnum.TOPIC_NAME_INVALID.getErrorCode(), assertThrows(BizException.class, () -> controller.create(request)).getErrorCode());
        request.setName("1234567890123");
        assertEquals(ResponseCodeEnum.TOPIC_NAME_TOO_LONG.getErrorCode(), assertThrows(BizException.class, () -> controller.create(request)).getErrorCode());
        verifyNoInteractions(topics);
    }
    @Test void existingTopicReturnsDuplicateError() {
        ChannelTopicController controller = new ChannelTopicController();
        TopicDOMapper topics = mock(TopicDOMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        ReflectionTestUtils.setField(controller, "topicMapper", topics); ReflectionTestUtils.setField(controller, "redis", redis);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(topics.findEnabledByName("话题")).thenReturn(TopicDO.builder().id(1L).name("话题").build());
        TopicCreateReqVO request = new TopicCreateReqVO(); request.setName(" 话题 ");
        assertEquals(ResponseCodeEnum.TOPIC_ALREADY_EXISTS.getErrorCode(), assertThrows(BizException.class, () -> controller.create(request)).getErrorCode());
        verify(topics, never()).insertTopic(anyString());
    }
}
