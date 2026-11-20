package com.shrimp.comment.biz.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.shrimp.comment.biz.constants.MQConstants;
import com.shrimp.comment.biz.constants.RedisKeyConstants;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.shrimp.comment.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.comment.biz.enums.CommentLevelEnum;
import com.shrimp.comment.biz.enums.CommentLikeLuaResultEnum;
import com.shrimp.comment.biz.enums.LikeUnlikeCommentTypeEnum;
import com.shrimp.comment.biz.enums.ResponseCodeEnum;
import com.shrimp.comment.biz.model.dto.DelayDeleteCommentCacheMqDTO;
import com.shrimp.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import com.shrimp.comment.biz.model.dto.PublishCommentMqDTO;
import com.shrimp.comment.biz.model.vo.*;
import com.shrimp.comment.biz.retry.SendMqRetryHelper;
import com.shrimp.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.shrimp.comment.biz.rpc.KeyValueRpcService;
import com.shrimp.comment.biz.rpc.UserRpcService;
import com.shrimp.comment.biz.service.CommentService;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.constants.DateConstants;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.kv.dto.req.FindCommentContentReqDTO;
import com.shrimp.kv.dto.rsp.FindCommentContentRspDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private CommentLikeDOMapper commentLikeDOMapper;

    private static final DefaultRedisScript<Long> BATCH_ADD_HOT_CHILD_COMMENTS_SCRIPT;

    static {
        BATCH_ADD_HOT_CHILD_COMMENTS_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/batch_add_hot_child_comments.lua"));
        BATCH_ADD_HOT_CHILD_COMMENTS_SCRIPT.setScriptSource(resourceScriptSource);
        BATCH_ADD_HOT_CHILD_COMMENTS_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_COMMENT_LIKE_CHECK_SCRIPT;

    static {
        BLOOM_COMMENT_LIKE_CHECK_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_like_check.lua"));
        BLOOM_COMMENT_LIKE_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_COMMENT_LIKE_CHECK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_BATCH_ADD_COMMENT_LIKE_SCRIPT;

    private static final DefaultRedisScript<Long> BLOOM_COMMENT_UNLIKE_CHECK_SCRIPT;

    static {
        BLOOM_COMMENT_UNLIKE_CHECK_SCRIPT = new DefaultRedisScript<>();
        BLOOM_COMMENT_UNLIKE_CHECK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_unlike_check.lua")));
        BLOOM_COMMENT_UNLIKE_CHECK_SCRIPT.setResultType(Long.class);
    }

    static {
        BLOOM_BATCH_ADD_COMMENT_LIKE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_comment_like.lua"));
        BLOOM_BATCH_ADD_COMMENT_LIKE_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_BATCH_ADD_COMMENT_LIKE_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_ADD_COMMENT_LIKE_AND_EXPIRE_SCRIPT;

    static {
        BLOOM_ADD_COMMENT_LIKE_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_comment_like_and_expire.lua"));
        BLOOM_ADD_COMMENT_LIKE_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_ADD_COMMENT_LIKE_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Override
    public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
        String content = publishCommentReqVO.getContent();
        String imageUrl = publishCommentReqVO.getImageUrl();

        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl)
                , "评论正文和图片不能同时为空");

        Long userId = LoginUserContextHolder.getLoginUserId();
        if (userId == null || noteCountDOMapper.countCommentableNote(publishCommentReqVO.getNoteId(), userId) == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_COMMENTABLE);
        }
//        调用RPC
        String commentId = distributedIdGeneratorRpcService.getCommentId();
        PublishCommentMqDTO dto = PublishCommentMqDTO.builder()
                .noteId(publishCommentReqVO.getNoteId())
                .commentId(Long.valueOf(commentId))
                .content(content)
                .imageUrl(imageUrl)
                .replyCommentId(publishCommentReqVO.getReplyCommentId())
                .creatorId(userId)
                .createTime(LocalDateTime.now())
                .build();

        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(dto));
        return Response.success();
    }

    @SneakyThrows
    @Override
    public PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO findCommentPageListReqVO) {
        Long noteId = findCommentPageListReqVO.getNoteId();
        Integer pageNo = findCommentPageListReqVO.getPageNo();
        long pageSize = 10;

        String countNoteTotalKey = RedisKeyConstants.buildCountNoteTotalKey(noteId);
        Number redisCount = (Number) redisTemplate.opsForHash().get(countNoteTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL);
        long count = ObjUtil.isNull(redisCount) ? 0L : (Long) redisCount.longValue();

        if (ObjUtil.isNull(redisCount)) {
            Long dbCount = noteCountDOMapper.selectCommentTotalByNoteId(noteId);
            if (ObjUtil.isNull(dbCount) || dbCount <= 0) {
                threadPoolTaskExecutor.submit(() -> syncNoteCommentTotal2Redis(countNoteTotalKey, 0L));
                return PageResponse.success(null, pageNo, 0);
            }

            threadPoolTaskExecutor.submit(() -> syncNoteCommentTotal2Redis(countNoteTotalKey, dbCount));
            count = dbCount;
        }

        if (count == 0) {
            return PageResponse.success(null, pageNo, 0);
        }

//        要返回的参数列表
        List<FindCommentItemRspVO> findCommentItemRspVOList = Lists.newArrayList();
        long offset = PageResponse.getOffset(pageNo, pageSize);

        String commentListKey = RedisKeyConstants.buildCommentListKey(noteId);
        Boolean hasKey = redisTemplate.hasKey(commentListKey);
//        不存在该RedisKey
        if (!hasKey) {
//            异步同步数据到Redis
            threadPoolTaskExecutor.submit(() -> syncNoteHeatCommentId2Redis(commentListKey, noteId));
        }
//        存在该RedisKey,则走Cache查询
        if (hasKey && (offset + pageSize) <= 500) {
//          从heatKey获取ID集合
            Set<Object> commentIds = redisTemplate.opsForZSet()
                    .reverseRangeByScore(commentListKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize);
//            极小情况下，会查询不到结果
            if (CollUtil.isEmpty(commentIds)) {
                List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
                findCommentListAndAsync2Redis(oneLevelCommentDOS, noteId, findCommentItemRspVOList);
                asyncNoteCommentDetail2LocalCache(findCommentItemRspVOList);
                return PageResponse.success(findCommentItemRspVOList, pageNo, count);
            }

            List<Long> commentIdList = Lists.newArrayList(commentIds.stream()
                    .map(id -> Long.valueOf(id.toString())).toList());

//            本地缓存查询
//            从本地获取数据集合,遍历集合，得到本地不存在的数据ID
            List<Long> expiredLocalCacheIds = Lists.newArrayList();
            for (Long commentId : commentIdList) {
                String cached = LOCAL_CACHE.getIfPresent(commentId);
                if (StringUtils.isBlank(cached)) {
                    expiredLocalCacheIds.add(commentId);
                } else {
                    findCommentItemRspVOList.add(JsonUtils.parseJSONString(cached, FindCommentItemRspVO.class));
                }
            }
//            如需要，则进行下一步查询
            if (CollUtil.isEmpty(expiredLocalCacheIds)) {
                findCommentItemRspVOList = findCommentItemRspVOList.stream().sorted(Comparator.comparing(FindCommentItemRspVO::getHeat).reversed()).collect(Collectors.toList());

                setCommentCountData(findCommentItemRspVOList, expiredLocalCacheIds);
                return PageResponse.success(findCommentItemRspVOList, pageNo, count);
            }
//            Redis查询

            log.info("==>需要从Redis中查询数据");

            commentIdList = expiredLocalCacheIds;
//          根据ID集合转换成对应的key集合
            List<String> commentDetailKeys = commentIdList.stream().map(RedisKeyConstants::buildCommentDetailKey).toList();
//          发起MGet，如果ID对应的评论没有，会返回null
            List<Object> commentDetailCaches = redisTemplate.opsForValue().multiGet(commentDetailKeys);
//          查询出不存在的Cache
            List<Long> expiredRedisCommentIds = Lists.newArrayList();
            for (int i = 0; i < commentDetailCaches.size(); i++) {
                String jsonStr = (String) commentDetailCaches.get(i);
                if (ObjUtil.isNotNull(jsonStr)) {
                    FindCommentItemRspVO findCommentItemRspVO = JsonUtils.parseJSONString(jsonStr, FindCommentItemRspVO.class);
                    findCommentItemRspVOList.add(findCommentItemRspVO);
                } else {
                    expiredRedisCommentIds.add(commentIdList.get(i));
                }
            }
//          若需要，则从DB中查询
            if (!CollUtil.isEmpty(expiredRedisCommentIds)) {
                List<CommentDO> needQueryCommentFromDB = commentDOMapper.selectByCommentIds(expiredRedisCommentIds);
                findCommentListAndAsync2Redis(needQueryCommentFromDB, noteId, findCommentItemRspVOList);
            }

            findCommentItemRspVOList = findCommentItemRspVOList.stream().sorted(Comparator.comparing(FindCommentItemRspVO::getHeat).reversed()).collect(Collectors.toList());
            asyncNoteCommentDetail2LocalCache(findCommentItemRspVOList);

            setCommentCountData(findCommentItemRspVOList, expiredRedisCommentIds);
            return PageResponse.success(findCommentItemRspVOList, pageNo, count);
        }

//        查询分页一级评论
        List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
        findCommentListAndAsync2Redis(oneLevelCommentDOS, noteId, findCommentItemRspVOList);
        asyncNoteCommentDetail2LocalCache(findCommentItemRspVOList);
        return PageResponse.success(findCommentItemRspVOList, pageNo, count);
    }

    @Override
    public PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(FindChildCommentPageListReqVO findChildCommentPageListReqVO) {
//        父评论ID
        Long parentCommentId = findChildCommentPageListReqVO.getParentCommentId();
//        当前页码
        Integer pageNo = findChildCommentPageListReqVO.getPageNo();
//        每页展示的二级评论数
        long pageSize = 6;
//        先从缓存查询
        String countCommentKey = RedisKeyConstants.buildCountCommentKey(parentCommentId);
        Object childCommentTotalObj = redisTemplate.opsForHash().get(countCommentKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
        Long childCommentTotal = null;
        if (ObjUtil.isNull(childCommentTotalObj)) {
//        查询一级评论下子评论的总数
            childCommentTotal = commentDOMapper.selectChildCommentTotalById(parentCommentId);
            if (ObjUtil.isNull(childCommentTotal)) {
                throw new BizException(ResponseCodeEnum.PARENT_COMMENT_NOT_FOUND);
            }
//         异步到Redis中
            threadPoolTaskExecutor.submit(() -> syncCountCommentTotal2redis(countCommentKey, parentCommentId));
        } else {
            childCommentTotal = Long.valueOf(childCommentTotalObj.toString());
        }

        if (childCommentTotal == 0L) {
            return PageResponse.success(null, pageNo, 0L);
        }

//        分页返参VO
        List<FindChildCommentItemRspVO> childCommentRspVOs = Lists.newArrayList();
//         计算分页查询的偏移量 offset (需要 +1，因为最早回复的二级评论已经被展示了)
        long offset = PageResponse.getOffset(pageNo, pageSize)
                + (Boolean.TRUE.equals(findChildCommentPageListReqVO.getIncludeFirst()) ? 0 : 1);
//      构建子评论分页zset KEY
        String childCommentListKey = RedisKeyConstants.buildChildCommentListKey(parentCommentId);
        Boolean hasKey = redisTemplate.hasKey(childCommentListKey);
        if (!hasKey) {
            threadPoolTaskExecutor.submit(() -> syncChildCommentList2Redis(childCommentListKey, parentCommentId));
        }
        if (hasKey && offset <= 60 - pageSize) {
//            从Redis中获取zset数据(查询出来的set数据不是有序的)
            Set<Object> childCommentIds = redisTemplate.opsForZSet().rangeByScore(childCommentListKey, 0, Double.POSITIVE_INFINITY, offset, pageSize);
//            缓存可能早于新回复建立；数量不足时整页回查，避免返回不完整的分页。
            long expectedSize = Math.min(pageSize, Math.max(0L, childCommentTotal - offset));
            if (CollUtil.isEmpty(childCommentIds) || childCommentIds.size() < expectedSize) {
                List<CommentDO> childCommentDOs = commentDOMapper.selectChildPageList(parentCommentId, offset, pageSize);
                findChildCommentAndAsync2Redis(childCommentDOs, childCommentRspVOs);

                return PageResponse.success(childCommentRspVOs, pageNo, childCommentTotal, pageSize);
            }
//            处理STRING
//            转化set为long,将其转化为KEY
            List<Long> childCommentIdsList = childCommentIds.stream().map(obj -> Long.valueOf(obj.toString())).toList();
            List<String> commentDetailKeyList = childCommentIdsList.stream()
                    .map(RedisKeyConstants::buildChildCommentDetailKey).toList();
//            获取缓存数据List
            List<Object> commentDetailObjList = redisTemplate.opsForValue().multiGet(commentDetailKeyList);
//            设置过期ID集合
            List<Long> expiredRedisChildCommentIds = Lists.newArrayList();
//            遍历缓存数据
            for (int i = 0; i < childCommentIdsList.size(); i++) {
                Long childCommentId = childCommentIdsList.get(i);
                Object commentObj = commentDetailObjList.get(i);
//            记录过期的集合
                if (ObjUtil.isNull(commentObj)) {
                    expiredRedisChildCommentIds.add(childCommentId);
                    continue;
                }
                FindChildCommentItemRspVO rspVO = JsonUtils.parseJSONString(commentObj.toString(), FindChildCommentItemRspVO.class);
                childCommentRspVOs.add(rspVO);
            }
//            如果有过期的数据，则进行下一步
            if (CollUtil.isNotEmpty(expiredRedisChildCommentIds)) {
//          根据评论ID查询对应的数据
                List<CommentDO> childCommentDOS = commentDOMapper.selectTwoLevelCommentByIds(expiredRedisChildCommentIds);
//            DB查询，并且同步到缓存中
                findChildCommentAndAsync2Redis(childCommentDOS, childCommentRspVOs);
            }

            childCommentRspVOs = childCommentRspVOs.stream().sorted(Comparator.comparing(FindChildCommentItemRspVO::getCommentId)).toList();
            return PageResponse.success(childCommentRspVOs, pageNo, childCommentTotal, pageSize);
        }

//        分页查询子评论
        List<CommentDO> childCommentDOs = commentDOMapper.selectChildPageList(parentCommentId, offset, pageSize);
        findChildCommentAndAsync2Redis(childCommentDOs, childCommentRspVOs);
        return PageResponse.success(childCommentRspVOs, pageNo, childCommentTotal, pageSize);
    }

    private static final DefaultRedisScript<Long> RELEASE_COMMENT_LIKE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    @Override
    public Response<?> likeComment(LikeCommentReqVO request) {
        return changeCommentLike(request.getCommentId(), true);
    }

    @Override
    public Response<?> unlikeComment(UnlikeCommentReqVO request) {
        return changeCommentLike(request.getCommentId(), false);
    }

    private Response<?> changeCommentLike(Long commentId, boolean like) {
        checkCommentIsExist(commentId);
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (userId == null) throw new BizException(ResponseCodeEnum.PARAM_VALID_FAIL);
        String stateKey = "comment:like:state:" + userId + ":" + commentId;
        String lockKey = stateKey + ":lock";
        String owner = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, owner, 30, TimeUnit.SECONDS))) {
            throw new BizException(ResponseCodeEnum.COMMENT_LIKE_BUSY);
        }
        try {
            Object state = redisTemplate.opsForValue().get(stateKey);
            boolean currentlyLiked = state == null
                    ? commentLikeDOMapper.selectCountByCommentIdAndUserId(commentId, userId) > 0
                    : Boolean.TRUE.equals(state);
            if (currentlyLiked == like) {
                if (like) throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKE);
                throw new BizException(ResponseCodeEnum.COMMENT_UNLIKE);
            }
            LikeUnlikeCommentMqDTO dto = LikeUnlikeCommentMqDTO.builder().commentId(commentId)
                    .userId(userId).createTime(LocalDateTime.now())
                    .type(like ? LikeUnlikeCommentTypeEnum.LIKE.getCode() : LikeUnlikeCommentTypeEnum.UNLIKE.getCode()).build();
            String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" +
                    (like ? MQConstants.TAG_LIKE : MQConstants.TAG_UNLIKE);
            Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
            // Holding the lock until broker acknowledgement preserves send order for this pair.
            rocketMQTemplate.syncSendOrderly(destination, message, String.valueOf(userId));
            redisTemplate.opsForValue().set(stateKey, like, 7, TimeUnit.DAYS);
            return Response.success();
        } finally {
            redisTemplate.execute(RELEASE_COMMENT_LIKE_LOCK, Collections.singletonList(lockKey), owner);
        }
    }

    @Override
    public Response<?> deleteComment(DeleteCommentReqVO deleteCommentReqVO) {
        Long commentId = deleteCommentReqVO.getCommentId();
//        检查要删除的评论是否存在
        CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);
        if (ObjUtil.isNull(commentDO)) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
//        检查用户是否有权限删除此评论
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (!ObjUtil.equals(commentDO.getUserId(), userId)) {
            throw new BizException(ResponseCodeEnum.NOT_PERMIT_DELETE_COMMENT);
        }
//    TODO  远程删除和删除DB可以并行执行

//        远程调用删除评论内容
        if (!commentDO.getIsContentEmpty()) {
            boolean resultFlag = keyValueRpcService.deleteCommentContent(commentDO);
//        若调用结果错误，则return
            if (!resultFlag) {
                return Response.fail(new BizException(ResponseCodeEnum.DELETE_COMMENT_CONTENT_FAIL));
            }
        }
//        删除DB评论数据
        int deleteCount = commentDOMapper.deleteByPrimaryKey(commentId);
        if (deleteCount == 0) {
            return Response.success();
        }
//        删除zset的ID
        Integer level = commentDO.getLevel();
//        构建zset Key 和 STRING的详情缓存KEY
        String listIdKey = null;
        String commentDetailKey = null;
        if (ObjUtil.equals(level, CommentLevelEnum.ONE.getCode())) {
            listIdKey = RedisKeyConstants.buildCommentListKey(commentDO.getNoteId());
            commentDetailKey = RedisKeyConstants.buildCommentDetailKey(commentId);
        } else if (ObjUtil.equals(level, CommentLevelEnum.TWO.getCode())) {
            listIdKey = RedisKeyConstants.buildChildCommentListKey(commentDO.getParentId());
            commentDetailKey = RedisKeyConstants.buildChildCommentDetailKey(commentId);
        } else {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        redisTemplate.opsForZSet().remove(listIdKey, commentId);
//        删除STRING的详情缓存（Redis和本地）
        redisTemplate.delete(commentDetailKey);
//        幂等操作
        if (ObjUtil.equals(level, CommentLevelEnum.ONE.getCode())) {
            LOCAL_CACHE.invalidate(commentId);
        }
//       发送Mq，延迟删除缓存(延迟一秒)
        DelayDeleteCommentCacheMqDTO delayDeleteCommentCacheMqDTO = DelayDeleteCommentCacheMqDTO.builder()
                .commentId(commentId)
                .level(level)
                .parentId(commentDO.getParentId())
                .noteId(commentDO.getNoteId())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(delayDeleteCommentCacheMqDTO)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_DELAY_DELETE_CACHE, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("## 延时删除 Redis 评论缓存消息发送成功...");
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("## 延时删除 Redis 评论缓存消息发送失败...", e);
                    }
                },
                3000,
                1);
//        发送Mq，删除相关数据
        message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentDO)).build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELETE_COMMENT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论删除】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论删除】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public void removeCommentLocalCache(Long commentId) {
        LOCAL_CACHE.invalidate(commentId);
    }

    private void syncBloomCommentLike2Redis(String bloomCommentLikeKey, Long userId) {
//        查询当前用户点赞过的评论 ID
        List<Long> args = commentLikeDOMapper.selectCommentIdsByUserId(userId);
        try {
            if (CollUtil.isEmpty(args)) {
                return;
            }
            Long expireTime = 60 * 60 + RandomUtil.randomLong(4 * 60 * 60);
            args.add(expireTime);
//        执行redis脚本，批量同步布隆过滤器的数据
            redisTemplate.execute(BLOOM_BATCH_ADD_COMMENT_LIKE_SCRIPT, Collections.singletonList(bloomCommentLikeKey), args.toArray());
        } catch (Exception e) {
            log.error("## 异步初始化【评论点赞】布隆过滤器异常: ", e);
        }
    }

    private void checkCommentIsExist(Long commentId) {
//        先在本地缓存中查找
        String commentJSON = LOCAL_CACHE.getIfPresent(commentId);
//        若不在，则到Redis查询
        if (StringUtils.isBlank(commentJSON)) {
//        先查一级评论以及首评论
            String commentDetailKey = RedisKeyConstants.buildCommentDetailKey(commentId);
            Object commentDetailObj = redisTemplate.opsForValue().get(commentDetailKey);
            if (ObjUtil.isNotNull(commentDetailObj)) {
                return;
            }
//        再查找二级评论
            String childCommentDetailKey = RedisKeyConstants.buildChildCommentDetailKey(commentId);
            Object childCommentDetailObj = redisTemplate.opsForValue().get(childCommentDetailKey);
            if (ObjUtil.isNotNull(childCommentDetailObj)) {
                return;
            }
        }
//        都没有查找到，则到DB查询
        CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);
        if (ObjUtil.isNull(commentDO)) {
//        若没有，则抛出错误
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
//        TODO 可以做异步将评论同步到Redis缓存中
    }

    private void findChildCommentAndAsync2Redis(List<CommentDO> childCommentDOs, List<FindChildCommentItemRspVO> childCommentRspVOs) {
        List<FindCommentContentReqDTO> findCommentContentReqDTOs = Lists.newArrayList();
        Set<Long> userIds = Sets.newHashSet();

//        归属的笔记ID
        Long noteId = null;

//        循环提取RPC的调用需要的参数
        for (CommentDO childCommentDO : childCommentDOs) {
            noteId = childCommentDO.getNoteId();
            Boolean isContentEmpty = childCommentDO.getIsContentEmpty();
            if (!Boolean.TRUE.equals(isContentEmpty)) {
                FindCommentContentReqDTO findCommentContentReqDTO = FindCommentContentReqDTO.builder()
                        .contentId(childCommentDO.getContentUuid())
                        .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(childCommentDO.getCreateTime()))
                        .build();
                findCommentContentReqDTOs.add(findCommentContentReqDTO);
            }
//            构建用户ID
            userIds.add(childCommentDO.getUserId());

            Long replyUserId = childCommentDO.getReplyUserId();
            if (replyUserId != null && childCommentDO.getReplyCommentId() != null
                    && !ObjUtil.equals(childCommentDO.getParentId(), childCommentDO.getReplyCommentId())) {
                userIds.add(replyUserId);
            }
        }

        List<FindCommentContentRspDTO> findCommentContentRspDTOS = findCommentContentReqDTOs.isEmpty()
                ? Collections.emptyList()
                : keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOs);

        Map<String, String> commentUuidAndContentMap = null;
        if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
            commentUuidAndContentMap = findCommentContentRspDTOS.stream()
                    .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, FindCommentContentRspDTO::getContent));
        }

        List<FindUserByIdRspDTO> findUserByIdRspDTOList = userRpcService.findUserByIds(Lists.newArrayList(userIds));
        Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
        if (CollUtil.isNotEmpty(findUserByIdRspDTOList)) {
            userIdAndDTOMap = findUserByIdRspDTOList.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));
        }
//      整合数据
        for (CommentDO childCommentDO : childCommentDOs) {
            Long userId = childCommentDO.getUserId();
            FindChildCommentItemRspVO findChildCommentItemRspVO = FindChildCommentItemRspVO.builder()
                    .userId(userId)
                    .commentId(childCommentDO.getId())
                    .imageUrl(childCommentDO.getImageUrl())
                    .createTime(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
                    .likeTotal(childCommentDO.getLikeTotal())
                    .build();

            ChildCommentUserAssembler.apply(childCommentDO, findChildCommentItemRspVO, userIdAndDTOMap);

            if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
                String contentUuid = childCommentDO.getContentUuid();
                if (StringUtils.isNotBlank(contentUuid)) {
                    String content = commentUuidAndContentMap.get(contentUuid);
                    findChildCommentItemRspVO.setContent(content);
                }
            }
            childCommentRspVOs.add(findChildCommentItemRspVO);
        }
//        异步到Redis中
        threadPoolTaskExecutor.submit(() ->
                syncChildCommentDetail2Redis(childCommentRspVOs));
    }

    private void syncChildCommentDetail2Redis(List<FindChildCommentItemRspVO> findChildCommentItemRspVO) {
//        转化为map
        Map<String, String> keyAndJSONMap = Maps.newHashMap();
        for (FindChildCommentItemRspVO rspVO : findChildCommentItemRspVO) {
            Long commentId = rspVO.getCommentId();
            String key = RedisKeyConstants.buildChildCommentDetailKey(commentId);

            String jsonString = JsonUtils.toJsonString(rspVO);
            keyAndJSONMap.put(key, jsonString);
        }
//        Redis pipelined
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
//                TODO operations实际调用spring环境中的RedisTemplate
                ValueOperations valueOperations = operations.opsForValue();
                for (Map.Entry<String, String> entry : keyAndJSONMap.entrySet()) {
                    valueOperations.set(entry.getKey(), entry.getValue());
                    long expiredTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
                    operations.expire(entry.getKey(), expiredTime, TimeUnit.SECONDS);
                }
                return null;
            }
        });
    }

    private void syncChildCommentList2Redis(String childCommentListKey, Long parentCommentId) {
//        从DB中查询数据
        List<CommentDO> commentDOS = commentDOMapper.selectChildCommentByParentIdAndLimit(parentCommentId, 60L);
//        redis pipelined批量插入
        int size = commentDOS.size();
        Object[] args = new Object[size * 2 + 1];
        for (int i = 0; i < size; i++) {
            CommentDO commentDO = commentDOS.get(i);
            args[2 * i] = DateUtils.localDateTime2Timestamp(commentDO.getCreateTime());
            args[2 * i + 1] = commentDO.getId();
        }
        long expiredTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
        args[2 * size] = expiredTime;

        redisTemplate.execute(BATCH_ADD_HOT_CHILD_COMMENTS_SCRIPT, Collections.singletonList(childCommentListKey), args);
    }

    private void syncCountCommentTotal2redis(String countCommentKey, Long parentCommentId) {
        Long childCommentTotal = commentDOMapper.selectChildCommentTotalById(parentCommentId);
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                operations.opsForHash().put(countCommentKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, childCommentTotal);
                long expiredTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
                operations.expire(countCommentKey, expiredTime, TimeUnit.SECONDS);
                return null;
            }
        });
    }

    private void findCommentListAndAsync2Redis(List<CommentDO> oneLevelCommentDOS, Long noteId, List<FindCommentItemRspVO> findCommentItemRspVOList) throws InterruptedException, ExecutionException {
        if (CollUtil.isEmpty(oneLevelCommentDOS)) return;
        //        获取一级评论的最早回复ID
        List<Long> firstReplyCommentIds = oneLevelCommentDOS.stream()
                .map(CommentDO::getFirstReplyCommentId).filter(Objects::nonNull).distinct().toList();
//        查询起对应的二级评论
        List<CommentDO> twoLevelCommentDOS = firstReplyCommentIds.isEmpty()
                ? Collections.emptyList() : commentDOMapper.selectTwoLevelCommentByIds(firstReplyCommentIds);
//        注意匹对评论数据
        Map<Long, CommentDO> twoCommentMapById = twoLevelCommentDOS.stream()
                .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));

        List<CommentDO> needQueryAllComment = Lists.newArrayList();
        needQueryAllComment.addAll(oneLevelCommentDOS);
        needQueryAllComment.addAll(twoLevelCommentDOS);
//        处理评论内容的远程调用
        CompletableFuture<List<FindCommentContentRspDTO>> findCommentContentRspDTOSFuture =
                CompletableFuture.supplyAsync(() -> getFindCommentContentRspDTOS(needQueryAllComment, noteId), threadPoolTaskExecutor);

//        处理用户信息的远程调用
        CompletableFuture<List<FindUserByIdRspDTO>> findUserByIdRspDTOListFuture = CompletableFuture.supplyAsync(() -> getFindUserByIdRspDTOS(needQueryAllComment));
//        处理RPC调用返回的数据
//        comment_content
        List<FindCommentContentRspDTO> findCommentContentRspDTOS = findCommentContentRspDTOSFuture.get();
        if (ObjUtil.isNull(findCommentContentRspDTOS)) {
            throw new BizException(ResponseCodeEnum.RPC_KEY_VALUE_GET_COMMENT_CONTENT_FAIL);
        }

        Map<String, String> CommentContentMapById = findCommentContentRspDTOS.stream()
                .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, dto -> dto.getContent()));
//        user_info
        List<FindUserByIdRspDTO> findUserByIdRspDTOList = findUserByIdRspDTOListFuture.get();
        if (ObjUtil.isNull(findUserByIdRspDTOList)) {
            throw new BizException(ResponseCodeEnum.RPC_KEY_VALUE_GET_USER_FAIL);
        }
        Map<Long, FindUserByIdRspDTO> userMapById = findUserByIdRspDTOList.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));

//        填充findCommentItemRspVOList
        for (CommentDO oneCommentDO : oneLevelCommentDOS) {
            Long firstReplyCommentId = oneCommentDO.getFirstReplyCommentId();
            CommentDO twoCommentDO = null;
            if (CollUtil.isNotEmpty(twoCommentMapById)) {
                twoCommentDO = twoCommentMapById.get(firstReplyCommentId);
            }
            FindUserByIdRspDTO findUserByIdRspDTO;
            String content = null;
            FindCommentItemRspVO twoRspVO = null;
            if (ObjUtil.isNotNull(twoCommentDO)) {
//                id, user_id, content_uuid, is_content_empty, image_url, like_total, create_time
                findUserByIdRspDTO = userMapById.get(twoCommentDO.getUserId());
                if (Boolean.FALSE.equals(twoCommentDO.getIsContentEmpty())) {
                    content = CommentContentMapById.get(twoCommentDO.getContentUuid());
                }

                twoRspVO = FindCommentItemRspVO.builder()
                        .commentId(twoCommentDO.getId())
                        .content(content)
                        .imageUrl(twoCommentDO.getImageUrl())
                        .likeTotal(twoCommentDO.getLikeTotal())
                        .heat(twoCommentDO.getHeat())
                        .createTime(DateUtils.formatRelativeTime(twoCommentDO.getCreateTime()))
//                    处理用户信息
                        .userId(twoCommentDO.getUserId())
                        .avatar(findUserByIdRspDTO == null ? null : findUserByIdRspDTO.getAvatar())
                        .nickname(findUserByIdRspDTO == null ? null : findUserByIdRspDTO.getNickName())
                        .build();
            }
//
            content = null;
            findUserByIdRspDTO = userMapById.get(oneCommentDO.getUserId());

            if (Boolean.FALSE.equals(oneCommentDO.getIsContentEmpty())) {
                content = CommentContentMapById.get(oneCommentDO.getContentUuid());
            }

            FindCommentItemRspVO oneRspVO = FindCommentItemRspVO.builder()
                    .commentId(oneCommentDO.getId())
                    .content(content)
                    .imageUrl(oneCommentDO.getImageUrl())
                    .likeTotal(oneCommentDO.getLikeTotal())
                    .heat(oneCommentDO.getHeat())
                    .createTime(DateUtils.formatRelativeTime(oneCommentDO.getCreateTime()))
//                    处理用户信息
                    .userId(oneCommentDO.getUserId())
                    .avatar(findUserByIdRspDTO == null ? null : findUserByIdRspDTO.getAvatar())
                    .nickname(findUserByIdRspDTO == null ? null : findUserByIdRspDTO.getNickName())
                    .childCommentTotal(oneCommentDO.getChildCommentTotal())
                    .firstReplyComment(twoRspVO)
                    .build();
//      TODO 如何处理置顶评论呢
            findCommentItemRspVOList.add(oneRspVO);
        }

//        异步到Redis中
        threadPoolTaskExecutor.submit(() -> {
            long expireTime = RandomUtil.randomInt(5 * 60 * 60);
//            redis通道
            redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                ValueOperations<String, Object> operations = redisTemplate.opsForValue();
                for (FindCommentItemRspVO rspVO : findCommentItemRspVOList) {
                    Long commentId = rspVO.getCommentId();
                    String commentDetailKey = RedisKeyConstants.buildCommentDetailKey(commentId);
                    operations.set(commentDetailKey, JsonUtils.toJsonString(rspVO), expireTime, TimeUnit.SECONDS);
                }
                return null;
            });
        });
    }

    private List<FindUserByIdRspDTO> getFindUserByIdRspDTOS(List<CommentDO> needQueryAllComment) {
        List<Long> userIds = Lists.newArrayList();
        userIds.addAll(needQueryAllComment.stream()
                .map(CommentDO::getUserId).toList());

        List<FindUserByIdRspDTO> findUserByIdRspDTOList = userRpcService.findUserByIds(userIds);
        return findUserByIdRspDTOList;
    }

    private List<FindCommentContentRspDTO> getFindCommentContentRspDTOS
            (List<CommentDO> needQueryAllComment, Long noteId) {
        List<FindCommentContentReqDTO> findCommentContentReqDTOList = Lists.newArrayList();
//        过滤内容为空的评论
        findCommentContentReqDTOList.addAll(
                needQueryAllComment.stream()
                        .filter(commentDO -> Boolean.FALSE.equals(commentDO.getIsContentEmpty()))
                        .map(commentDO ->
                                FindCommentContentReqDTO.builder()
                                        .yearMonth(commentDO.getCreateTime().format(DateConstants.DATE_FORMAT_Y_M))
                                        .contentId(commentDO.getContentUuid())
                                        .build()
                        ).toList()
        );
        if (findCommentContentReqDTOList.isEmpty()) return Collections.emptyList();
        return keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOList);
    }

    public void asyncNoteCommentDetail2LocalCache(List<FindCommentItemRspVO> findCommentItemRspVOS) {
        log.info("==> 开始异步将数据加载到本地缓存中");
        threadPoolTaskExecutor.submit(() -> {
            for (FindCommentItemRspVO rspDTO : findCommentItemRspVOS) {
                Long commentId = rspDTO.getCommentId();
                String jsonString = JsonUtils.toJsonString(rspDTO);
                LOCAL_CACHE.put(commentId, jsonString);
            }
        });
    }

    public void syncNoteCommentTotal2Redis(String noteCommentTotalKey, Long dbCount) {
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                operations.opsForHash()
                        .putIfAbsent(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, dbCount);

//                随机过期时间（保底1小时+随机时间），单位：秒
                long expireTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
                operations.expire(noteCommentTotalKey, expireTime, TimeUnit.SECONDS);
                return null;
            }
        });
    }

    public void syncNoteHeatCommentId2Redis(String commentListKey, Long noteId) {
        List<CommentDO> commentDOS = commentDOMapper.selectHeatComments(noteId);

        if (CollUtil.isNotEmpty(commentDOS)) {
            redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
                for (CommentDO commentDO : commentDOS) {
                    zSetOperations.add(commentListKey, commentDO.getId(), commentDO.getHeat());
                }
                int randomExpiryTime = RandomUtil.randomInt(5 * 60 * 60); // 5小时以内
                redisTemplate.expire(commentListKey, randomExpiryTime, TimeUnit.SECONDS);
                return null;
            });
        }
    }

    private void setChildCommentCountData(List<FindChildCommentItemRspVO> childCommentItemRspVOList,
                                          List<Long> expiredChildCommentIds) {
//        提取需要查询的ID集合
        List<Long> childCommentIdList = childCommentItemRspVOList.stream()
                .map(FindChildCommentItemRspVO::getCommentId).toList();
//        转化为评论计数RedisKEY
        List<String> childCommentCountKey = childCommentIdList.stream()
                .map(RedisKeyConstants::buildChildCommentDetailKey).toList();
//        从Redis中查询评论计数Hash数据
        Map<Long, Map<Object, Object>> childCommentIdAndCountDataMap =
                getCommentCountDataMapAndAsync2RedisHash(childCommentCountKey, childCommentIdList);
//        遍历VO,设置评论的点赞数
        for (FindChildCommentItemRspVO rspVO : childCommentItemRspVOList) {
            Long commentId = rspVO.getCommentId();

            if (CollUtil.isNotEmpty(expiredChildCommentIds) &&
                    expiredChildCommentIds.contains(commentId)) {
                continue;
            }

            Map<Object, Object> map = childCommentIdAndCountDataMap.get(commentId);
            if (CollUtil.isNotEmpty(map)) {
                Object likeTotalObj = map.get(RedisKeyConstants.FIELD_LIKE_TOTAL);
                Long likeTotal = Long.valueOf(likeTotalObj.toString());
                rspVO.setLikeTotal(likeTotal);
            }
        }
    }

    /**
     * 设置评论 VO 的计数
     *
     * @param commentRspVOS     返参 VO 集合
     * @param expiredCommentIds 缓存中已失效的评论 ID 集合
     */
    private void setCommentCountData(List<FindCommentItemRspVO> commentRspVOS,
                                     List<Long> expiredCommentIds) {
        //准备从缓存中获取计数（子评论总数，喜欢总数)
        //缓存中存在的评论ID
        List<Long> notExpiredCommentIds = Lists.newArrayList();
        //遍历没有过期的
        for (FindCommentItemRspVO commentRspVO : commentRspVOS) {
            Long oneCommentId = commentRspVO.getCommentId();
            notExpiredCommentIds.add(oneCommentId);
            FindCommentItemRspVO firstReplyComment = commentRspVO.getFirstReplyComment();
            if (ObjUtil.isNotNull(firstReplyComment)) {
                Long twoCommentId = firstReplyComment.getCommentId();
                notExpiredCommentIds.add(twoCommentId);
            }
        }

//        获取Redis KEY
        List<String> redisCommentCountKey = notExpiredCommentIds.stream()
                .map(RedisKeyConstants::buildCountCommentKey)
                .toList();
//        从Redis中查询评论计数Hash数据
        Map<Long, Map<Object, Object>> commentIdAndCountMap = getCommentCountDataMapAndAsync2RedisHash(redisCommentCountKey, notExpiredCommentIds);
//        遍历 VO, 设置对应评论的二级评论数、点赞数
        for (FindCommentItemRspVO commentRspVO : commentRspVOS) {
//       若当前这条评论是从数据库中查询出来的, 则无需设置二级评论数、点赞数，以数据库查询出来的为主
            Long oneCommentId = commentRspVO.getCommentId();
            if (CollUtil.isNotEmpty(expiredCommentIds) &&
                    expiredCommentIds.contains(oneCommentId)) {
                continue;
            }

            Map<Object, Object> oneMap = commentIdAndCountMap.get(oneCommentId);
            if (CollUtil.isNotEmpty(oneMap)) {
                Object childCommentTotalObj = oneMap.get(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
                long childCommentTotal = ObjUtil.isNull(childCommentTotalObj) ? 0 : Long.valueOf(childCommentTotalObj.toString());
                commentRspVO.setChildCommentTotal(childCommentTotal);
                Object likeTotalObj = oneMap.get(RedisKeyConstants.FIELD_LIKE_TOTAL);
                long likeTotal = ObjUtil.isNull(likeTotalObj) ? 0 : Long.valueOf(likeTotalObj.toString());
                commentRspVO.setLikeTotal(likeTotal);

                FindCommentItemRspVO firstReplyComment = commentRspVO.getFirstReplyComment();
                if (ObjUtil.isNotNull(firstReplyComment)) {
                    Long twoCommentId = firstReplyComment.getCommentId();
                    Map<Object, Object> twoMap = commentIdAndCountMap.get(twoCommentId);
                    if (CollUtil.isNotEmpty(twoMap)) {
                        likeTotalObj = twoMap.get(RedisKeyConstants.FIELD_LIKE_TOTAL);
                        likeTotal = ObjUtil.isNull(likeTotalObj) ? 0 : Long.valueOf(likeTotalObj.toString());
                        firstReplyComment.setLikeTotal(likeTotal);
                    }
                }
            }
        }

    }

    private Map<Long, Map<Object, Object>> getCommentCountDataMapAndAsync2RedisHash(List<String> redisCommentCountKey, List<Long> notExpiredCommentIds) {
        //        设置Map接收Redis的键值对
        Map<Long, Map<Object, Object>> commentIdAndCountMap = Maps.newHashMap();
//        发起RedisPipelined获取ID计数数据
        List<Object> results = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                redisCommentCountKey.forEach(key -> {
                    // 在管道中执行 Redis 的 hash.entries 操作
                    // 此操作会获取指定 Hash 键中所有的字段和值
                    operations.opsForHash().entries(key);
                });
                return null;
            }
        });
//        遍历总的ID集合，提取其中过期的ID计数缓存
        List<Long> expiredCountCommentIds = Lists.newArrayList();
        for (int i = 0; i < notExpiredCommentIds.size(); i++) {
            Long currentCommentId = Long.valueOf(notExpiredCommentIds.get(i).toString());
            Map<Object, Object> hash = (Map<Object, Object>) results.get(i);
            if (CollUtil.isEmpty(hash)) {
                expiredCountCommentIds.add(currentCommentId);
            } else {
                commentIdAndCountMap.put(currentCommentId, hash);
            }
        }
//        查询DB获取到过期的数据
        if (CollUtil.isNotEmpty(expiredCountCommentIds)) {
            List<CommentDO> commentDOS = commentDOMapper.selectCommentCountByIds(expiredCountCommentIds);
            //        将数据添加到map中
            for (CommentDO commentDO : commentDOS) {
                Long commentID = commentDO.getId();
                Integer level = commentDO.getLevel();
                Map<Object, Object> hash = CommentLevelEnum.ONE.getCode().equals(level) ?
                        Map.of(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, commentDO.getChildCommentTotal(), RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal()) :
                        Map.of(RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());

                commentIdAndCountMap.put(commentID, hash);
            }
//            通过RedisPipelined，异步到Redis缓存
            //      遍历从DB查询到的数据
            threadPoolTaskExecutor.submit(() -> {
                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public Object execute(RedisOperations operations) {
                        commentDOS.forEach(commentDO -> {
                            Long commentId = commentDO.getId();
                            String key = RedisKeyConstants.buildCountCommentKey(commentId);
                            Integer level = commentDO.getLevel();
                            Map<Object, Object> hash = CommentLevelEnum.ONE.getCode().equals(level) ?
                                    Map.of(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, commentDO.getChildCommentTotal(), RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal()) :
                                    Map.of(RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());
                            operations.opsForHash().putAll(key, hash);

                            long expireTime = RandomUtil.randomInt(5 * 60 * 60);
                            operations.expire(key, expireTime, TimeUnit.SECONDS);
                        });
                        return null;
                    }
                });
            });
        }
        return commentIdAndCountMap;
    }
}
