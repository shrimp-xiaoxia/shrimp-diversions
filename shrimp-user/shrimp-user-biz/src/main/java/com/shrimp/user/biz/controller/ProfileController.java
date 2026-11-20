package com.shrimp.user.biz.controller;

import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.enums.ResponseCodeEnum;
import com.shrimp.user.biz.model.vo.FindProfileReqVO;
import com.shrimp.user.biz.model.vo.FindProfileRspVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@RestController
public class ProfileController {
    @Resource private UserDOMapper users;
    @Resource private CountFeignApi counts;

    @PostMapping("/user/profile")
    public Response<FindProfileRspVO> profile(@Validated @RequestBody FindProfileReqVO request) {
        Long id = request.getUserId() == null ? LoginUserContextHolder.getLoginUserId() : request.getUserId();
        if (id == null) throw new BizException(ResponseCodeEnum.LOGIN_STATUS_ERROR);
        UserDO user = users.selectByPrimaryKey(id);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted()) || (user.getStatus() != null && user.getStatus() != 0))
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        FindProfileRspVO result = new FindProfileRspVO();
        result.setUserId(String.valueOf(id)); result.setNickname(user.getNickname()); result.setAvatar(user.getAvatar());
        result.setXiaohashuId(user.getXiaohashuId()); result.setSex(user.getSex()); result.setBirthday(user.getBirthday());
        result.setIntroduction(user.getIntroduction());
        if (user.getBirthday() != null) result.setAge(Period.between(user.getBirthday(), LocalDate.now()).getYears());
        try {
            CountQueryReqDTO query = new CountQueryReqDTO(); query.setIds(List.of(id));
            Response<List<CountResultDTO>> response = counts.findUsers(query);
            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                CountResultDTO count = response.getData().get(0);
                result.setFansTotal(count.getFansTotal()); result.setFansTotalAvailable(count.getFansTotal() != null);
                result.setFollowingTotal(count.getFollowingTotal()); result.setFollowingTotalAvailable(count.getFollowingTotal() != null);
                result.setNoteTotal(count.getNoteTotal()); result.setNoteTotalAvailable(count.getNoteTotal() != null);
                if (count.getLikeTotal() != null && count.getCollectTotal() != null) {
                    result.setLikeAndCollectTotal(count.getLikeTotal() + count.getCollectTotal());
                    result.setLikeAndCollectTotalAvailable(true);
                }
            }
        } catch (RuntimeException ignored) { /* basic profile remains available */ }
        return Response.success(result);
    }
}
