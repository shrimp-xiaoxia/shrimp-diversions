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
import com.shrimp.comment.biz.enums.ResponseCodeEnum;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    @Autowired
    private CommentLikeDOMapper commentLikeDOMapper;

    @Override
    public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
        String content = publishCommentReqVO.getContent();
        String imageUrl = publishCommentReqVO.getImageUrl();

        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl)
                , "评论正文和图片不能同时为空");

        Long userId = LoginUserContextHolder.getLoginUserId();
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
            Map<Long, String> CommentIdAndDetailJsonMap = LOCAL_CACHE.getAll(commentIdList, missingKeys -> {
                Map<Long, String> missingData = Maps.newHashMap();
                for (Long missingKey : missingKeys) {
                    expiredLocalCacheIds.add(missingKey);
                    missingData.put(missingKey, Strings.EMPTY);
                }
                return missingData;
            });
//            如需要，则进行下一步查询
            if (CollUtil.isEmpty(expiredLocalCacheIds)) {
                findCommentItemRspVOList.addAll(CommentIdAndDetailJsonMap.values()
                        .stream()
                        .map(str -> JsonUtils.parseJSONString(str, FindCommentItemRspVO.class))
                        .toList());
                findCommentItemRspVOList = findCommentItemRspVOList.stream().sorted(Comparator.comparing(FindCommentItemRspVO::getHeat).reversed()).collect(Collectors.toList());
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
//        TODO 先从缓存查询

//        查询一级评论下子评论的总数
        Long childCommentTotalById = commentDOMapper.selectChildCommentTotalById(parentCommentId);
        if (ObjUtil.isNull(childCommentTotalById) || childCommentTotalById == 0L) {
            return PageResponse.success(null, pageNo, 0L);
        }
//        分页返参VO
        List<FindChildCommentItemRspVO> childCommentRspVOs = Lists.newArrayList();
//         计算分页查询的偏移量 offset (需要 +1，因为最早回复的二级评论已经被展示了)
        long offset = PageResponse.getOffset(pageNo, pageSize);
//        分页查询子评论
        List<CommentDO> childCommentDOs = commentDOMapper.selectChildPageList(parentCommentId, offset, pageSize);

        List<FindCommentContentReqDTO> findCommentContentReqDTOs = Lists.newArrayList();
        Set<Long> userIds = Sets.newHashSet();

//        归属的笔记ID
        Long noteId = null;

//        循环提取RPC的调用需要的参数
        for (CommentDO childCommentDO : childCommentDOs) {
            noteId = childCommentDO.getNoteId();
            Boolean isContentEmpty = childCommentDO.getIsContentEmpty();
            if (!isContentEmpty) {
                FindCommentContentReqDTO findCommentContentReqDTO = FindCommentContentReqDTO.builder()
                        .contentId(childCommentDO.getContentUuid())
                        .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(childCommentDO.getCreateTime()))
                        .build();
                findCommentContentReqDTOs.add(findCommentContentReqDTO);
            }
//            构建用户ID
            userIds.add(childCommentDO.getUserId());

            Long parentId = childCommentDO.getParentId();
            Long replyUserId = childCommentDO.getReplyUserId();
            if (!ObjUtil.equals(parentId, replyUserId)) {
                userIds.add(replyUserId);
            }
        }

        List<FindCommentContentRspDTO> findCommentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOs);

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
//        TODO
        for (CommentDO childCommentDO : childCommentDOs) {
            Long userId = childCommentDO.getUserId();
            FindChildCommentItemRspVO findChildCommentItemRspVO = FindChildCommentItemRspVO.builder()
                    .userId(userId)
                    .commentId(childCommentDO.getId())
                    .imageUrl(childCommentDO.getImageUrl())
                    .content(childCommentDO.getContentUuid())
                    .createTime(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
                    .likeTotal(childCommentDO.getLikeTotal())
                    .build();

            if (CollUtil.isNotEmpty(userIdAndDTOMap)) {
                FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
                if (ObjUtil.isNull(findUserByIdRspDTO)) {
                    findChildCommentItemRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                    findChildCommentItemRspVO.setNickname(findUserByIdRspDTO.getNickName());
                }

                Long parentId = childCommentDO.getParentId();
                Long replyCommentId = childCommentDO.getReplyCommentId();
                if (ObjUtil.isNotNull(replyCommentId) && !ObjUtil.equals(replyCommentId, parentId)) {
                    Long replyUserId = childCommentDO.getReplyUserId();
                    findUserByIdRspDTO = userIdAndDTOMap.get(replyUserId);
                    if (ObjUtil.isNull(findUserByIdRspDTO)) {
                        findChildCommentItemRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                        findChildCommentItemRspVO.setNickname(findUserByIdRspDTO.getNickName());
                    }
                }
            }

            if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
                String contentUuid = childCommentDO.getContentUuid();
                if (StringUtils.isNotBlank(contentUuid)) {
                    String content = commentUuidAndContentMap.get(contentUuid);
                    findChildCommentItemRspVO.setContent(content);
                }
            }

            childCommentRspVOs.add(findChildCommentItemRspVO);
        }

        return PageResponse.success(childCommentRspVOs, pageNo, childCommentTotalById, pageSize);
    }

    private void findCommentListAndAsync2Redis(List<CommentDO> oneLevelCommentDOS, Long noteId, List<FindCommentItemRspVO> findCommentItemRspVOList) throws InterruptedException, ExecutionException {
        //        获取一级评论的最早回复ID
        List<Long> firstReplyCommentIds = oneLevelCommentDOS.stream()
                .map(CommentDO::getFirstReplyCommentId).toList();
//        查询起对应的二级评论
        List<CommentDO> twoLevelCommentDOS = commentDOMapper.selectTwoLevelCommentByIds(firstReplyCommentIds);
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
                        .avatar(findUserByIdRspDTO.getAvatar())
                        .nickname(findUserByIdRspDTO.getNickName())
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
                    .avatar(findUserByIdRspDTO.getAvatar())
                    .nickname(findUserByIdRspDTO.getNickName())
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
        List<FindCommentContentRspDTO> findCommentContentRspDTOS = Lists.newArrayList();
        findCommentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOList);
        return findCommentContentRspDTOS;
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
                        .put(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, dbCount);

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
}
