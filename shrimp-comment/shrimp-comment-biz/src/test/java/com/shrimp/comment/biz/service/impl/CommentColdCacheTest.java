package com.shrimp.comment.biz.service.Impl;

import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.rpc.KeyValueRpcService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentColdCacheTest {
    @Test void commentListUsesCountServiceNoteKey() {
        assertEquals("count:note:1895795420446070644", RedisKeyConstants.buildCountNoteTotalKey(1895795420446070644L));
    }

    @Test void imageOnlyCommentDoesNotCallContentService() {
        var service = new CommentServiceImpl();
        var rpc = mock(KeyValueRpcService.class);
        ReflectionTestUtils.setField(service, "keyValueRpcService", rpc);
        var imageOnly = CommentDO.builder().isContentEmpty(true).build();
        List<?> result = ReflectionTestUtils.invokeMethod(service, "getFindCommentContentRspDTOS", List.of(imageOnly), 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(rpc);
    }

    @Test void emptyPageDoesNotQueryCommentsOrExternalServices() {
        var service = new CommentServiceImpl();
        var mapper = mock(CommentDOMapper.class);
        var rpc = mock(KeyValueRpcService.class);
        ReflectionTestUtils.setField(service, "commentDOMapper", mapper);
        ReflectionTestUtils.setField(service, "keyValueRpcService", rpc);
        var output = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(service, "findCommentListAndAsync2Redis", List.of(), 1L, output);
        assertTrue(output.isEmpty());
        verifyNoInteractions(mapper, rpc);
    }
}
