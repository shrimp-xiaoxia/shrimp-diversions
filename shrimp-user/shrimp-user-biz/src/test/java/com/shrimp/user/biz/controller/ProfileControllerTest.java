package com.shrimp.user.biz.controller;
import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.model.vo.FindProfileReqVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class ProfileControllerTest {
    @AfterEach void clear() { LoginUserContextHolder.removeLoginUserContext(); }
    @Test void returnsBasicProfileWhenCountsAreUnavailable() {
        ProfileController controller = new ProfileController();
        UserDOMapper users = mock(UserDOMapper.class);
        CountFeignApi counts = mock(CountFeignApi.class);
        ReflectionTestUtils.setField(controller, "users", users); ReflectionTestUtils.setField(controller, "counts", counts);
        when(users.selectByPrimaryKey(4L)).thenReturn(UserDO.builder().id(4L).nickname("name").birthday(LocalDate.of(2000,1,1)).build());
        when(counts.findUsers(any())).thenThrow(new IllegalStateException());
        FindProfileReqVO request = new FindProfileReqVO(); request.setUserId(4L);
        var response = controller.profile(request);
        assertTrue(response.isSuccess()); assertEquals("4", response.getData().getUserId());
        assertNull(response.getData().getFansTotal()); assertFalse(response.getData().isFansTotalAvailable());
        assertNull(response.getData().getFollowingTotal()); assertFalse(response.getData().isFollowingTotalAvailable());
        assertNull(response.getData().getLikeAndCollectTotal()); assertFalse(response.getData().isLikeAndCollectTotalAvailable());
    }
    @Test void aggregatesLikeAndCollectCounts() {
        ProfileController controller = new ProfileController();
        UserDOMapper users = mock(UserDOMapper.class); CountFeignApi counts = mock(CountFeignApi.class);
        ReflectionTestUtils.setField(controller, "users", users); ReflectionTestUtils.setField(controller, "counts", counts);
        LoginUserContextHolder.setLoginUserId(4L);
        when(users.selectByPrimaryKey(4L)).thenReturn(UserDO.builder().id(4L).build());
        CountResultDTO value = new CountResultDTO(); value.setId(4L); value.setLikeTotal(3L); value.setCollectTotal(5L);
        when(counts.findUsers(any())).thenReturn(Response.success(List.of(value)));
        var profile = controller.profile(new FindProfileReqVO()).getData();
        assertEquals(8L, profile.getLikeAndCollectTotal()); assertTrue(profile.isLikeAndCollectTotalAvailable());
    }
}
