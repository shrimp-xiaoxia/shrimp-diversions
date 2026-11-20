package com.shrimp.note.biz.service.impl;

import com.shrimp.framework.common.exception.BizException;
import com.shrimp.note.biz.domain.dataobject.ChannelDO;
import com.shrimp.note.biz.domain.mapper.ChannelDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.PublishNoteReqVO;
import com.shrimp.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.shrimp.note.biz.rpc.KeyValueRpcService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PublishNoteServiceTest {
    private final NoteServiceImpl service = new NoteServiceImpl();
    private final ChannelDOMapper channels = mock(ChannelDOMapper.class);
    private final NoteDOMapper notes = mock(NoteDOMapper.class);
    private final DistributedIdGeneratorRpcService ids = mock(DistributedIdGeneratorRpcService.class);
    private final KeyValueRpcService contents = mock(KeyValueRpcService.class);
    private final RocketMQTemplate mq = mock(RocketMQTemplate.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "channelDOMapper", channels);
        ReflectionTestUtils.setField(service, "noteDOMapper", notes);
        ReflectionTestUtils.setField(service, "distributedIdGeneratorRpcService", ids);
        ReflectionTestUtils.setField(service, "keyValueRpcService", contents);
        ReflectionTestUtils.setField(service, "rocketMQTemplate", mq);
    }

    @Test
    void rejectsMissingOrDeletedChannelBeforeAnyWrites() {
        PublishNoteReqVO request = request();
        BizException missing = assertThrows(BizException.class, () -> service.publishNote(request));
        assertEquals(ResponseCodeEnum.CHANNEL_NOT_FOUND.getErrorCode(), missing.getErrorCode());
        when(channels.selectByPrimaryKey(1L)).thenReturn(ChannelDO.builder().isDeleted(true).build());
        BizException deleted = assertThrows(BizException.class, () -> service.publishNote(request));
        assertEquals(ResponseCodeEnum.CHANNEL_NOT_FOUND.getErrorCode(), deleted.getErrorCode());
        verifyNoInteractions(ids, notes, contents, mq);
    }

    @Test
    void insertFailureCleansContentAndDoesNotSendMessageEvenIfCleanupThrows() {
        when(channels.selectByPrimaryKey(1L)).thenReturn(ChannelDO.builder().isDeleted(false).build());
        when(ids.getNoteId()).thenReturn("123");
        when(contents.addNoteContent(anyString(), anyString())).thenReturn(true);
        when(notes.insert(any())).thenThrow(new RuntimeException("insert failed"));
        when(contents.deleteNoteContent(anyString())).thenThrow(new RuntimeException("cleanup failed"));

        BizException failure = assertThrows(BizException.class, () -> service.publishNote(request()));
        assertEquals(ResponseCodeEnum.NOTE_PUBLISH_FAIL.getErrorCode(), failure.getErrorCode());
        verify(contents).deleteNoteContent(anyString());
        verifyNoInteractions(mq);
    }

    private PublishNoteReqVO request() {
        return PublishNoteReqVO.builder().channelId(1L).type(0).imgUris(List.of("image"))
                .title("title").content("content").build();
    }
}
