package com.shrimp.note.biz.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.constants.RedisKeyConstants;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.dataobject.TopicDO;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.TopicDOMapper;
import com.shrimp.note.biz.enums.NoteStatusEnum;
import com.shrimp.note.biz.enums.NoteTypeEnum;
import com.shrimp.note.biz.enums.NoteVisibleEnum;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.*;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;
import com.shrimp.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.shrimp.note.biz.rpc.KeyValueRpcService;
import com.shrimp.note.biz.rpc.UserRpcService;
import com.shrimp.note.biz.service.NoteService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private NoteDOMapper noteDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final Cache<Long, FindNoteDetailRspVO> LOCAL_NOTE_DETAIL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1000)
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        Long userId = LoginUserContextHolder.getLoginUserId();

        Integer type = publishNoteReqVO.getType();
        Preconditions.checkArgument(NoteTypeEnum.isValid(type), "该文章类型不合法");
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        NoteDO noteDO = new NoteDO();
//        检查图片
        noteDO.setType(Byte.valueOf(noteTypeEnum.getCode().toString()));
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUris = publishNoteReqVO.getImgUris();
                Preconditions.checkArgument(!imgUris.isEmpty(), "图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUris.size() <= 8, "笔记图片不能多于 8 张");
                StringBuffer sb = new StringBuffer();
                imgUris.forEach(imgUri -> {
                    sb.append(imgUri);
                    sb.append(",");
                });
                sb.deleteCharAt(sb.length() - 1);
                noteDO.setImgUris(sb.toString());
            }
//        检查视频
            case VIDEO -> {
                String videoUri = publishNoteReqVO.getVideoUri();
                Preconditions.checkArgument(!videoUri.isEmpty(), "视频不能为空");
                noteDO.setVideoUri(videoUri);
            }
            default -> {
            }
        }
//        生成ID
        String noteId = distributedIdGeneratorRpcService.getNoteId();
        Long id = Long.valueOf(noteId);
        noteDO.setId(id);

//        判断是否需要保存笔记内容
        String content = publishNoteReqVO.getContent();
        noteDO.setIsContentEmpty(Boolean.TRUE);
        UUID contentUuid = null;
        if (StringUtils.isNotBlank(content)) {
            contentUuid = UUID.randomUUID();
//        发起远程调用
            boolean flag = keyValueRpcService.addNoteContent(content, contentUuid.toString());
            if (!flag) {
                throw new BizException(ResponseCodeEnum.SAVE_NOTE_CONTENT_FAIL);
            }
            noteDO.setIsContentEmpty(Boolean.FALSE);
            noteDO.setContentUuid(contentUuid.toString());
        }
//        主题ID
        Long topicId = publishNoteReqVO.getTopicId();
        if (ObjUtil.isNotNull(topicId)) {
            noteDO.setTopicId(topicId);
            String topicName = topicDOMapper.selectTopicNameById(topicId);
            Preconditions.checkArgument(StringUtils.isNoneBlank(topicName), "该话题类型不存在或者已经被删除了");
            noteDO.setTopicName(topicName);
        }
//        其他信息完善
        noteDO.setTitle(publishNoteReqVO.getTitle());
        noteDO.setCreatorId(userId);
        noteDO.setStatus(Byte.valueOf(NoteStatusEnum.NORMAL.getCode().toString()));
        noteDO.setIsTop(Boolean.FALSE);
        noteDO.setVisible(Byte.valueOf(NoteVisibleEnum.PUBLIC.getCode().toString()));
        noteDO.setCreateTime(LocalDateTime.now());
        noteDO.setUpdateTime(LocalDateTime.now());
//        保存DB
        try {
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);
            if (contentUuid != null) {
                keyValueRpcService.deleteNoteContent(contentUuid.toString());
            }
        }
        return Response.success();
    }

    @SneakyThrows
    @Override
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
//        获取ID
        Long id = findNoteDetailReqVO.getId();
//        userId 可以为null
        Long userId = LoginUserContextHolder.getLoginUserId();

//        本地缓存中取
        FindNoteDetailRspVO noteDetailLocalCache = LOCAL_NOTE_DETAIL_CACHE.getIfPresent(id);
        if (ObjUtil.isNotNull(noteDetailLocalCache)) {
            log.info("==>本地缓存获取到的数据：{}", noteDetailLocalCache);
            return Response.success(noteDetailLocalCache);
        }
//        从缓存中取
        String noteDetailCacheKey = RedisKeyConstants.buildNoteDetailCacheKey(id);
        String JSONStr = (String) redisTemplate.opsForValue().get(noteDetailCacheKey);
        if (StringUtils.isNotBlank(JSONStr)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseJSONString(JSONStr, FindNoteDetailRspVO.class);

//            放入到本地缓存中
            threadPoolTaskExecutor.submit(() -> {
                LOCAL_NOTE_DETAIL_CACHE.put(id, findNoteDetailRspVO);
            });

            return Response.success(findNoteDetailRspVO);
        }

//        查询笔记数据
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(id);
//        若无则有错误
        if (ObjUtil.isNull(noteDO)) {
//            缓存空值
            threadPoolTaskExecutor.execute(() -> {
                int ttl = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteDetailCacheKey, "null", ttl, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }

        Byte status = noteDO.getStatus();
        NoteStatusEnum noteStatusEnum = NoteStatusEnum.valueOf(Integer.valueOf(status));
        if (ObjUtil.isNull(noteStatusEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_STATUS_FAIL);
        }
//        TODO 判断当前笔记是否为待审核
//        TODO 这些被删除的笔记还可以被创建者看见吗？
        switch (noteStatusEnum) {
//        删除，给出错误信息
            case DELETED -> throw new BizException(ResponseCodeEnum.NOTE_DELETE);
//        下架，给出错误信息
            case DOWNED -> throw new BizException(ResponseCodeEnum.NOTE_TAKE_OFF);
            default -> {
            }
        }

        Long creatorId = noteDO.getCreatorId();
        Byte visible = noteDO.getVisible();
        NoteVisibleEnum noteVisibleEnum = NoteVisibleEnum.valueOf(Integer.valueOf(visible));
        //不公开，看是否登录ID是否等于creatorID
        if (ObjUtil.isNotNull(noteVisibleEnum) && noteVisibleEnum.equals(NoteVisibleEnum.PRIVATE)) {
            if (ObjUtil.isNull(userId) || !userId.equals(creatorId)) {
                //若不等于，则抛出错误
                throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
            }
        }

//        根据creatorId发起远程调用
        CompletableFuture<FindUserByIdRspDTO> userResultFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findUserById(creatorId), threadPoolTaskExecutor);
//        若该笔记内容不为空，根据contentUUId发起远程调用
        CompletableFuture<FindNoteContentRspDTO> contentResultFuture = CompletableFuture.completedFuture(null);
        if (!noteDO.getIsContentEmpty()) {
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()), threadPoolTaskExecutor);
        }
//         根据type来判断，该笔记的类型
        Byte type = noteDO.getType();
        Preconditions.checkArgument(NoteTypeEnum.isValid(Integer.valueOf(type)), "该笔记类型不合法");
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(Integer.valueOf(type));
//        imag
        List<String> imagUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                String imgUris = noteDO.getImgUris();
                imagUris = Arrays.stream(imgUris.split(",")).toList();
            }
//        video
            case VIDEO -> videoUri = noteDO.getVideoUri();
            default -> {
            }
        }
//        处理当前的数据
        String title = noteDO.getTitle();
        Long topicId = noteDO.getTopicId();
        String topicName = noteDO.getTopicName();
        LocalDateTime updateTime = noteDO.getUpdateTime();

//      处理并发调用的两个RPC
        log.info("==>处理并发调用的两个RPC");
//        user的详细信息
        FindUserByIdRspDTO findUserByIdRspDTO = userResultFuture.get();
        if (ObjUtil.isNull(findUserByIdRspDTO)) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_FIND_FAIL);
        }
//        笔记内容
        String content = null;
//        笔记内容不为空时，才要获取
        if (!noteDO.getIsContentEmpty()) {
            FindNoteContentRspDTO findNoteContentRspDTO = contentResultFuture.get();
//        若不存在，则抛出错误
//            TODO 出现两重if
            if (ObjUtil.isNull(findNoteContentRspDTO)) {
                throw new BizException(ResponseCodeEnum.NOTE_CONTENT_FIND_FAIL);
            }
            content = findNoteContentRspDTO.getContent();
        }

        FindNoteDetailRspVO findNoteDetailRspVO = FindNoteDetailRspVO.builder()
                .id(id)
                .title(title)
                .type(Integer.valueOf(type))
                .imgUris(imagUris)
                .videoUri(videoUri)
                .topicId(topicId) //不需要判断该话题是否被删除了
                .topicName(topicName)
                .visible(Integer.valueOf(visible))
                .updateTime(updateTime)
                .avatar(findUserByIdRspDTO.getAvatar())
                .creatorName(findUserByIdRspDTO.getNickName())
                .content(content)
                .build();

//        缓存数据
        threadPoolTaskExecutor.submit(() -> {
            int ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            String jsonString = JsonUtils.toJsonString(findNoteDetailRspVO);
            redisTemplate.opsForValue().set(noteDetailCacheKey, jsonString, ttl, TimeUnit.SECONDS);
        });
//        返回结果
        return Response.success(findNoteDetailRspVO);
    }

    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
//        获取ID
        Long id = updateNoteReqVO.getId();
        Long loginUserId = LoginUserContextHolder.getLoginUserId();
//        查询更新前的noteDetail
        NoteDO oldNoteDO = noteDOMapper.selectByPrimaryKey(id);

        if (ObjUtil.isNull(oldNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
        if (!ObjUtil.equals(oldNoteDO.getCreatorId(), loginUserId)) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_FAIL);
        }
//       检查content
        String content = updateNoteReqVO.getContent();
        boolean isContentEmpty = true;
        String contentUuid = null;
        CompletableFuture<Boolean> contentResultFuture = CompletableFuture.completedFuture(null);
//      不为空，异步调用
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
            contentUuid = oldNoteDO.getIsContentEmpty() ? UUID.randomUUID().toString() : oldNoteDO.getContentUuid();
            final String uuid = contentUuid;
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.addNoteContent(content, uuid), threadPoolTaskExecutor);
        } else if (!oldNoteDO.getIsContentEmpty()) {
//       VO中content为空，并且旧数据有笔记内容，异步调用删除其旧值
            final String uuid = oldNoteDO.getContentUuid();
//            更新成功，删除笔记对应的旧值，异步调用 TODO 投递到消息队列中
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.deleteNoteContent(uuid), threadPoolTaskExecutor);
        }
//        根据对应的topicID查询topicName
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (ObjUtil.isNotNull(topicId)) {
            TopicDO topicDO = topicDOMapper.selectByPrimaryKey(topicId);
//        检查
            Preconditions.checkArgument(ObjUtil.isNotNull(topicDO), "该话题ID不存在");
            topicName = topicDO.getName();
        }
//        type
        Integer type = updateNoteReqVO.getType();
        Preconditions.checkArgument(NoteTypeEnum.isValid(type), "该笔记类型错误");
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        String imagUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
//        imag（检查参数）
            case IMAGE_TEXT -> {
                List<String> uris = updateNoteReqVO.getImgUris();
                Preconditions.checkArgument(uris != null && !uris.isEmpty(), "该笔记图片不能为空");
                Preconditions.checkArgument(uris.size() <= 8, "该图片数量不能超过8张");

                imagUris = StringUtils.join(uris, ",");
            }
//        video(检查参数)
            case VIDEO -> {
                String uri = updateNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(uri), "该视频不能为空");
                videoUri = uri;
            }
            default -> {
            }
        }

//         获取异步调用保存content的结果
        if (!isContentEmpty) {
            Boolean b = contentResultFuture.get();
//        失败，则抛出错误
            if (ObjUtil.equals(b, Boolean.FALSE)) {
                throw new BizException(ResponseCodeEnum.NOTE_CONTENT_FIND_FAIL);
            }
        }

        NoteDO newNoteDO = NoteDO.builder()
                .id(id)
                .title(updateNoteReqVO.getTitle())
                .type(type.byteValue())
                .imgUris(imagUris)
                .videoUri(videoUri)
                .topicId(topicId)
                .topicName(topicName)
                .isContentEmpty(isContentEmpty)
                .contentUuid(contentUuid)
                .updateTime(LocalDateTime.now())
                .build();

        try {
//        DB更新
            noteDOMapper.updateByPrimaryKeySelective(newNoteDO);
        } catch (Exception e) {
//        更新失败，则要删除之前的笔记内容
            log.error("==>更新笔记失败:{}", updateNoteReqVO);
            if (ObjUtil.isNotNull(contentUuid)) {
//                这里直接就同步调用 TODO 可以使用消息队列
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
            return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
        }

//        删除缓存策略
        deleteCache(id);

        return Response.success();
    }

    private void deleteCache(Long id) {
        //        保证数据一致性
//        删除Redis缓存
        String noteDetailCacheKey = RedisKeyConstants.buildNoteDetailCacheKey(id);
        redisTemplate.delete(noteDetailCacheKey);
//        删除本地缓存
//        LOCAL_NOTE_DETAIL_CACHE.invalidate(id);
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, id);
        log.info("====> MQ：删除笔记本地缓存发送成功...");
//        延迟双删
        Message<String> message = MessageBuilder.withPayload(String.valueOf(id)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("## 延时删除 Redis 笔记缓存消息发送失败...", e);
                    }
                },
                3000,
                1);
    }

    @Override
    public void deleteNoteDetailLocalCache(Long noteId) {
        LOCAL_NOTE_DETAIL_CACHE.invalidate(noteId);
    }

    @Override
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        Long id = deleteNoteReqVO.getId();
        Long loginUserId = LoginUserContextHolder.getLoginUserId();
        NoteDO oldNote = noteDOMapper.selectByPrimaryKey(id);
//        检查笔记是否存在
        if (ObjUtil.isNull(oldNote)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
//        检查该用户是否有权限删除
        if (!ObjUtil.equals(oldNote.getCreatorId(), loginUserId)) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_FAIL);
        }

//        标记DB存储的笔记为已经删除状态
        NoteDO noteDO = NoteDO.builder()
                .id(id)
                .status(Byte.valueOf(NoteStatusEnum.DELETED.getCode().toString()))
                .updateTime(LocalDateTime.now())
                .build();
        int result = noteDOMapper.updateByPrimaryKeySelective(noteDO);
//        根据更新结果判断笔记是否存在
//        如果不存在，则抛出异常=
        if (result == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
//        删除缓存策略
        deleteCache(id);
//        返回结果
        return Response.success();
    }


    @Override
    public Response<?> updateNoteVisibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
//        提取ID
        Long id = updateNoteVisibleOnlyMeReqVO.getId();
        Long userId = LoginUserContextHolder.getLoginUserId();
        NoteDO oldNote = noteDOMapper.selectByPrimaryKey(id);
//        检查笔记是否存在
        if (ObjUtil.isNull(oldNote)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
//        检查该用户是否有权限删除
        if (!ObjUtil.equals(oldNote.getCreatorId(), userId)) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_FAIL);
        }
//        根据ID更新
        NoteDO noteDO = NoteDO.builder()
                .id(id)
                .visible(Byte.valueOf(NoteVisibleEnum.PRIVATE.getCode().toString()))
                .status(Byte.valueOf(NoteStatusEnum.NORMAL.getCode().toString()))
                .creatorId(userId)
                .updateTime(LocalDateTime.now())
                .build();
        int result = noteDOMapper.updateNoteVisible(noteDO);
//        根据更新结果来判断
        if (result == 0) {
//        失败,则抛出异常
            throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }
//        删除缓存
        deleteCache(id);
        return Response.success();
    }

    @Override
    public Response<?> updateNoteIsTop(UpdateNoteIsTopReqVO updateNoteIsTopReqVO) {
//        提取ID
        Long id = updateNoteIsTopReqVO.getId();
        Long userId = LoginUserContextHolder.getLoginUserId();
        NoteDO oldNote = noteDOMapper.selectByPrimaryKey(id);
//        检查笔记是否存在
        if (ObjUtil.isNull(oldNote)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
//        检查该用户是否有权限删除
        if (!ObjUtil.equals(oldNote.getCreatorId(), userId)) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_FAIL);
        }
//        更新数据
        NoteDO noteDO = NoteDO.builder()
                .id(id)
                .isTop(updateNoteIsTopReqVO.getIsTop())
                .creatorId(userId)
                .updateTime(LocalDateTime.now())
                .status(Byte.valueOf(NoteStatusEnum.NORMAL.getCode().toString()))
                .build();
        int result = noteDOMapper.updateNoteIsTop(noteDO);
        if (result == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_IS_TOP_FAIL);
        }
        deleteCache(id);
        return Response.success();
    }
}