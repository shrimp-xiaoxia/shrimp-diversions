package com.shrimp.note.biz.controller;

import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.domain.dataobject.ChannelDO;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.mapper.ChannelDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.DiscoverReqVO;
import com.shrimp.note.biz.model.vo.req.ProfileNotesReqVO;
import com.shrimp.note.biz.model.vo.rsp.NoteCardRspVO;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class NoteListController {
    @Resource private NoteDOMapper notes;
    @Resource private ChannelDOMapper channels;
    @Resource private UserFeignApi users;
    @Resource private CountFeignApi counts;
    @Resource private NoteLikeDOMapper likes;

    @PostMapping("/discover/note/list")
    public PageResponse<NoteCardRspVO> discover(@Validated @RequestBody DiscoverReqVO request) {
        if (channels.selectEnabled().isEmpty()) throw new BizException(ResponseCodeEnum.CHANNEL_NOT_FOUND);
        Long channelId = request.getChannelId();
        if (channelId != null && channelId != 0) {
            ChannelDO channel = channels.selectByPrimaryKey(channelId);
            if (channel == null || Boolean.TRUE.equals(channel.getIsDeleted())) throw new BizException(ResponseCodeEnum.CHANNEL_NOT_FOUND);
        }
        long total = notes.countDiscover(channelId);
        List<NoteDO> page = notes.findDiscover(channelId, PageResponse.getOffset(request.getPageNo(), 10));
        return PageResponse.success(page.isEmpty() ? null : cards(page), request.getPageNo(), total);
    }

    @PostMapping("/profile/note/list")
    public PageResponse<NoteCardRspVO> profile(@Validated @RequestBody ProfileNotesReqVO request) {
        boolean owner = request.getUserId().equals(LoginUserContextHolder.getLoginUserId());
        if (request.getType() != 1 && !owner) throw new BizException(ResponseCodeEnum.NOTE_CANT_FAIL);
        long total = notes.countProfile(request.getUserId(), request.getType(), owner);
        List<NoteDO> page = notes.findProfile(request.getUserId(), request.getType(), owner, PageResponse.getOffset(request.getPageNo(), 10));
        return PageResponse.success(page.isEmpty() ? null : cards(page), request.getPageNo(), total);
    }

    private List<NoteCardRspVO> cards(List<NoteDO> page) {
        Long viewerId = LoginUserContextHolder.getLoginUserId();
        Set<Long> likedIds = null;
        if (viewerId != null) {
            try {
                likedIds = Set.copyOf(likes.selectLikedNoteIds(viewerId, page.stream().map(NoteDO::getId).toList()));
            } catch (RuntimeException ignored) { /* state unavailable */ }
        }
        Set<Long> viewerLikedIds = likedIds;
        List<Long> creatorIds = page.stream().map(NoteDO::getCreatorId).distinct().toList();
        Response<List<FindUserByIdRspDTO>> response = users.findUserByIds(new FindUserByIdsReqDTO(creatorIds));
        if (response == null || !response.isSuccess() || response.getData() == null) throw new BizException(ResponseCodeEnum.CREATOR_FIND_FAIL);
        Map<Long, FindUserByIdRspDTO> creators = response.getData().stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, Function.identity(), (a,b) -> a));
        Map<Long, CountResultDTO> values = Collections.emptyMap();
        try {
            CountQueryReqDTO query = new CountQueryReqDTO(); query.setIds(page.stream().map(NoteDO::getId).toList());
            Response<List<CountResultDTO>> result = counts.findNotes(query);
            if (result != null && result.isSuccess() && result.getData() != null)
                values = result.getData().stream().collect(Collectors.toMap(CountResultDTO::getId, Function.identity(), (a,b) -> a));
        } catch (RuntimeException ignored) { /* count unavailable */ }
        Map<Long, CountResultDTO> countMap = values;
        return page.stream().map(note -> {
            NoteCardRspVO card = new NoteCardRspVO();
            if (viewerLikedIds != null) card.setIsLiked(viewerLikedIds.contains(note.getId()));
            card.setId(String.valueOf(note.getId())); card.setCreatorId(String.valueOf(note.getCreatorId()));
            card.setTitle(note.getTitle()); card.setType(note.getType().intValue()); card.setVisible(note.getVisible().intValue());
            if (note.getImgUris() != null && !note.getImgUris().isBlank()) card.setCover(note.getImgUris().split(",")[0]);
            card.setVideoUri(note.getVideoUri());
            FindUserByIdRspDTO creator = creators.get(note.getCreatorId());
            if (creator != null) { card.setNickname(creator.getNickName()); card.setAvatar(creator.getAvatar()); }
            CountResultDTO count = countMap.get(note.getId());
            if (count != null) { card.setLikeTotal(count.getLikeTotal()); card.setLikeTotalAvailable(count.getLikeTotal() != null); }
            return card;
        }).toList();
    }
}
