package com.shrimp.comment.biz.rpc;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRpcServiceTest {
    @Test void deduplicatesAndSplitsUserIdsByContractLimit() {
        var api = mock(UserFeignApi.class);
        when(api.findUserByIds(any())).thenAnswer(call -> {
            FindUserByIdsReqDTO request = call.getArgument(0);
            return Response.success(request.getIds().stream().map(id -> FindUserByIdRspDTO.builder().id(id).build()).toList());
        });
        var service = new UserRpcService();
        ReflectionTestUtils.setField(service, "userFeignApi", api);
        List<Long> ids = new ArrayList<>(LongStream.rangeClosed(1, 23).boxed().toList());
        ids.add(1L);
        assertEquals(23, service.findUserByIds(ids).size());
        var requests = ArgumentCaptor.forClass(FindUserByIdsReqDTO.class);
        verify(api, times(3)).findUserByIds(requests.capture());
        assertEquals(List.of(10, 10, 3), requests.getAllValues().stream().map(r -> r.getIds().size()).toList());
    }

    @Test void returnsNoPartialDataWhenABatchFails() {
        var api = mock(UserFeignApi.class);
        when(api.findUserByIds(any())).thenReturn(Response.success(List.of(FindUserByIdRspDTO.builder().id(1L).build())))
                .thenReturn(Response.fail("TEST-20100", "failure"));
        var service = new UserRpcService();
        ReflectionTestUtils.setField(service, "userFeignApi", api);
        assertNull(service.findUserByIds(LongStream.rangeClosed(1, 11).boxed().toList()));
    }
}
