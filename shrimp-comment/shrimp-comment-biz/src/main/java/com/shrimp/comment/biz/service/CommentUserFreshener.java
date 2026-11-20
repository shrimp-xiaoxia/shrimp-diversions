package com.shrimp.comment.biz.service;

import com.shrimp.comment.biz.model.vo.FindChildCommentItemRspVO;
import com.shrimp.comment.biz.model.vo.FindCommentItemRspVO;
import com.shrimp.comment.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Component
@Slf4j
public class CommentUserFreshener {
    private final UserRpcService users;

    public CommentUserFreshener(UserRpcService users) { this.users = users; }

    public void refreshTopLevel(List<FindCommentItemRspVO> comments) {
        List<FindCommentItemRspVO> all = new ArrayList<>();
        for (var comment : comments) {
            all.add(comment);
            if (comment.getFirstReplyComment() != null) all.add(comment.getFirstReplyComment());
        }
        Map<Long, List<Consumer<FindUserByIdRspDTO>>> targets = new HashMap<>();
        for (var item : all) add(targets, item.getUserId(), user -> {
            item.setAvatar(user.getAvatar());
            if (user.getNickName() != null) item.setNickname(user.getNickName());
        });
        refresh(targets);
    }

    public void refreshChildren(List<FindChildCommentItemRspVO> comments) {
        Map<Long, List<Consumer<FindUserByIdRspDTO>>> targets = new HashMap<>();
        for (var item : comments) {
            add(targets, item.getUserId(), user -> {
                item.setAvatar(user.getAvatar());
                if (user.getNickName() != null) item.setNickname(user.getNickName());
            });
            add(targets, item.getReplyUserId(), user -> {
                if (user.getNickName() != null) item.setReplyUserName(user.getNickName());
            });
        }
        refresh(targets);
    }

    private void add(Map<Long, List<Consumer<FindUserByIdRspDTO>>> targets, Long id, Consumer<FindUserByIdRspDTO> update) {
        if (id != null && id > 0) targets.computeIfAbsent(id, key -> new ArrayList<>()).add(update);
    }

    private void refresh(Map<Long, List<Consumer<FindUserByIdRspDTO>>> targets) {
        if (targets.isEmpty()) return;
        List<Long> ids = new ArrayList<>(targets.keySet());
        for (int start = 0; start < ids.size(); start += 10) {
            try {
                var profiles = users.findUserByIds(ids.subList(start, Math.min(start + 10, ids.size())));
                if (profiles == null) continue;
                for (var profile : profiles) {
                    if (profile == null || profile.getId() == null) continue;
                    for (var update : targets.getOrDefault(profile.getId(), List.of())) update.accept(profile);
                }
            } catch (RuntimeException ex) {
                log.warn("评论用户资料刷新失败，使用缓存资料");
            }
        }
    }
}
