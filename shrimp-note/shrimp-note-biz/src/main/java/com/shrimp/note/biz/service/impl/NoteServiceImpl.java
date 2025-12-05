package com.shrimp.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
import com.shrimp.note.biz.constants.MQConstants;
import com.shrimp.note.biz.constants.RedisKeyConstants;
import com.shrimp.note.biz.domain.dataobject.NoteCollectionDO;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.dataobject.NoteLikeDO;
import com.shrimp.note.biz.domain.dataobject.TopicDO;
import com.shrimp.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.domain.mapper.TopicDOMapper;
import com.shrimp.note.biz.enums.*;
import com.shrimp.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.shrimp.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.shrimp.note.biz.model.dto.OperateNoteMqDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    @Autowired
    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

    private static final Cache<Long, FindNoteDetailRspVO> LOCAL_NOTE_DETAIL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1000)
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static final DefaultRedisScript<Long> BLOOM_NOTE_LIKE_CHECK_SCRIPT;

    static {
        BLOOM_NOTE_LIKE_CHECK_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua"));
        BLOOM_NOTE_LIKE_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_NOTE_LIKE_CHECK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_BATCH_ADD_NOTE_LIKE_AND_EXPIRE_SCRIPT;

    static {
        BLOOM_BATCH_ADD_NOTE_LIKE_AND_EXPIRE_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua"));
        BLOOM_BATCH_ADD_NOTE_LIKE_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_BATCH_ADD_NOTE_LIKE_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> NOTE_LIKE_CHECK_AND_UPDATE_ZSET_SCRIPT;

    static {
        NOTE_LIKE_CHECK_AND_UPDATE_ZSET_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua"));
        NOTE_LIKE_CHECK_AND_UPDATE_ZSET_SCRIPT.setScriptSource(resourceScriptSource);
        NOTE_LIKE_CHECK_AND_UPDATE_ZSET_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> NOTE_LIKE_BATCH_ADD_AND_EXPIRE_ZSET_SCRIPT;

    static {
        NOTE_LIKE_BATCH_ADD_AND_EXPIRE_ZSET_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/note_like_batch_add_and_expire_zset.lua"));
        NOTE_LIKE_BATCH_ADD_AND_EXPIRE_ZSET_SCRIPT.setScriptSource(resourceScriptSource);
        NOTE_LIKE_BATCH_ADD_AND_EXPIRE_ZSET_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_NOTE_UNLIKE_CHECK_SCRIPT;

    static {
        BLOOM_NOTE_UNLIKE_CHECK_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_unlike_check.lua"));
        BLOOM_NOTE_UNLIKE_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_NOTE_UNLIKE_CHECK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_NOTE_COLLECT_CHECK_SCRIPT;

    static {
        BLOOM_NOTE_COLLECT_CHECK_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua"));
        BLOOM_NOTE_COLLECT_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_NOTE_COLLECT_CHECK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_BATCH_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT;

    static {
        BLOOM_BATCH_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua"));
        BLOOM_BATCH_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_BATCH_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT;

    static {
        BLOOM_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua"));
        BLOOM_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> NOTE_COLLECT_BATCH_ADD_AND_EXPIRE_SZET_SCRIPT;

    static {
        NOTE_COLLECT_BATCH_ADD_AND_EXPIRE_SZET_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/note_collect_batch_add_and_expire_zset.lua"));
        NOTE_COLLECT_BATCH_ADD_AND_EXPIRE_SZET_SCRIPT.setScriptSource(resourceScriptSource);
        NOTE_COLLECT_BATCH_ADD_AND_EXPIRE_SZET_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> NOTE_COLLECT_CHECK_AND_UPDATE_ZSET_SCRIPT;

    static {
        NOTE_COLLECT_CHECK_AND_UPDATE_ZSET_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua"));
        NOTE_COLLECT_CHECK_AND_UPDATE_ZSET_SCRIPT.setScriptSource(resourceScriptSource);
        NOTE_COLLECT_CHECK_AND_UPDATE_ZSET_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> BLOOM_NOTE_UNCOLLECT_CHECK_SCRIPT;

    static {
        BLOOM_NOTE_UNCOLLECT_CHECK_SCRIPT = new DefaultRedisScript();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_uncollect_check.lua"));
        BLOOM_NOTE_UNCOLLECT_CHECK_SCRIPT.setScriptSource(resourceScriptSource);
        BLOOM_NOTE_UNCOLLECT_CHECK_SCRIPT.setResultType(Long.class);
    }

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
//        删除可能存在因为缓存空值存储的null值，该方法的并发量不高,可以不使用MQ
        deleteCache(id);
//        发送MQ到计数服务
        OperateNoteMqDTO operateNoteMqDTO = OperateNoteMqDTO.builder()
                .noteId(id)
                .creatorId(userId)
                .type(NoteOperationEnum.PUBLISH.getCode()).build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(operateNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_COUNT_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记发布】MQ 发送异常: ", throwable);
            }
        });

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
//       发送MQ到计数服务
        OperateNoteMqDTO operateNoteMqDTO = OperateNoteMqDTO.builder()
                .noteId(id)
                //能够到这里，说明该登录用户跟该笔记得创建者是同一个
                .creatorId(loginUserId)
                .type(NoteOperationEnum.DELETE.getCode()).build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(operateNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_COUNT_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_DELETE;

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记删除】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记删除】MQ 发送异常: ", throwable);
            }
        });

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

    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
        Long noteId = likeNoteReqVO.getId();
//        查询要点赞的笔记存不存在,与调用findNoteDetail相比，业务场景不同，本方法在MYSQL查询不同，提升了性能
        Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
//        查询该用户是否已经点赞该笔记
        Long userId = LoginUserContextHolder.getLoginUserId();

        String bloomNoteLikeKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        String noteLikeZsetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        Long luaResult = redisTemplate.execute(BLOOM_NOTE_LIKE_CHECK_SCRIPT, Collections.singletonList(bloomNoteLikeKey), noteId);
        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteLikeLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (noteLikeLuaResultEnum) {
            case NOT_EXISTS -> {
//                查询该用户是否点赞该笔记
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
//                如果已经点赞则批量同步数据
                if (count > 0) {
                    asyncBatchAddDBNoteLike2Bloom(userId, expireSeconds, bloomNoteLikeKey);
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
//                如果没有 ,则批量同步数据和当前请求的参数
                asyncBatchAddDBAndArgsNoteLike2Bloom(userId, expireSeconds, bloomNoteLikeKey, noteId);
            }
            // 目标笔记已经被点赞 (可能存在误判，需要进一步确认)
            case NOTE_LIKED -> {
                Double score = redisTemplate.opsForZSet().score(noteLikeZsetKey, noteId);
                if (ObjUtil.isNotNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                if (count > 0) {
                    threadPoolTaskExecutor.submit(() -> {
                        batchAddNoteLikeZSet(userId);
                    });
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
            }
        }
//        构建时间
        LocalDateTime createTime = LocalDateTime.now();
//        执行Lua脚本，更新zset点赞列表
        luaResult = redisTemplate.execute(NOTE_LIKE_CHECK_AND_UPDATE_ZSET_SCRIPT, Collections.singletonList(noteLikeZsetKey), noteId, DateUtils.localDateTime2Timestamp(createTime));
//        根据返回结果执行相关逻辑
        noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteLikeLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        if (noteLikeLuaResultEnum.equals(NoteLikeLuaResultEnum.NOT_EXISTS)) {
//        如果不存在zset，则批量插入数据
            batchAddNoteLikeAndArgsNoteIdToZset(userId, noteId, createTime);
        }

//        发送MQ消息，将数据落库
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeTypeEnum.LIKE.getCode())
                .createTime(createTime)
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();

        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记点赞】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
//        提取noteId
        Long noteId = unlikeNoteReqVO.getId();
//        检查笔记是否存在
        Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
        Long userId = LoginUserContextHolder.getLoginUserId();
//        构建bloomKey
        String bloomUserNoteLikeKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
//        执行RedisScript
        Long luaResult = redisTemplate.execute(BLOOM_NOTE_UNLIKE_CHECK_SCRIPT, Collections.singletonList(bloomUserNoteLikeKey), noteId);
//        判断返回结果
        NoteUnlikeLuaResultEnum noteLikeLuaResultEnum = NoteUnlikeLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteLikeLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
//        不存在，则直接返回
        switch (noteLikeLuaResultEnum) {
            case NOTE_NOT_LIKE -> throw new BizException(ResponseCodeEnum.NOTE_UNLIKE);
            case NOT_EXISTS -> {
//        bloom不存在
//        查询数据库
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
//        异步同步bloom
                asyncBatchAddDBNoteLike2Bloom(userId, expireSeconds, bloomUserNoteLikeKey);
//        如果DB不存在，则返回
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_UNLIKE);
                }
            }
        }
//        允许误判已经点赞
        String noteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
//        去除zset中的已经点赞的数据
        redisTemplate.opsForZSet().remove(noteLikeZSetKey, noteId);
//        通知MQ
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeTypeEnum.UNLIKE.getCode())
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();

        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
        String hasKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hasKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        // 笔记ID
        Long noteId = collectNoteReqVO.getId();
        Long userId = LoginUserContextHolder.getLoginUserId();
        // 1. 校验被收藏的笔记是否存在
        Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
        //  2. 判断目标笔记，是否已经收藏过
        String bloomUserNoteCollectKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        Long luaResult = redisTemplate.execute(BLOOM_NOTE_COLLECT_CHECK_SCRIPT, Collections.singletonList(bloomUserNoteCollectKey), noteId);
        NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteCollectLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        switch (noteCollectLuaResultEnum) {
            case NOT_EXISTS -> {
                int count = noteCollectionDOMapper.selectCollectCountByUserIdAndNoteId(userId, noteId);
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                if (count > 0) {
                    threadPoolTaskExecutor.submit(() -> {
                        batchAddNoteCollectToBloom(userId, bloomUserNoteCollectKey, expireSeconds);
                    });
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECT);
                }

                redisTemplate.execute(BLOOM_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT, Collections.singletonList(bloomUserNoteCollectKey), noteId, expireSeconds);
                threadPoolTaskExecutor.submit(() -> {
                    batchAddNoteCollectToBloom(userId, bloomUserNoteCollectKey, expireSeconds);
                });
            }
            case NOTE_COLLECT -> {
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);
                if (ObjUtil.isNotNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECT);
                }
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                int count = noteCollectionDOMapper.selectCollectCountByUserIdAndNoteId(userId, noteId);
                if (count > 0) {
                    threadPoolTaskExecutor.submit(() -> batchAddNoteCollectDBToZSet(userId, userNoteCollectZSetKey, expireSeconds));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECT);
                }
            }
        }
        // 3. 更新用户 ZSET 收藏列表
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        LocalDateTime createTime = LocalDateTime.now();

        luaResult = redisTemplate.execute(NOTE_COLLECT_CHECK_AND_UPDATE_ZSET_SCRIPT, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(createTime));
        noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteCollectLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        if (NoteCollectLuaResultEnum.NOT_EXISTS.equals(noteCollectLuaResultEnum)) {
            batchAddNoteCollectDBAndArgsToZSet(userId, userNoteCollectZSetKey, expireSeconds, noteId, createTime);
        }
        // 4. 发送 MQ, 将收藏数据落库

        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .noteId(noteId)
                .userId(userId)
                .type(CollectUnCollectTypeEnum.COLLECT.getCode())
                .createTime(createTime)
                .noteCreatorId(creatorId)
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_COLLECT_OR_UNCOLLECTED + ":" + MQConstants.TAG_COLLECT;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("##发送[收藏笔记]落库成功，sendResult:{}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("##发送[收藏笔记]落库失败，throwable:{}", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO) {
//        提取ID
        Long noteId = unCollectNoteReqVO.getId();
        Long userId = LoginUserContextHolder.getLoginUserId();
//        检查笔记是否存在
        Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
//        检查该笔记是否已经被收藏
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        Long luaResult = redisTemplate.execute(BLOOM_NOTE_UNCOLLECT_CHECK_SCRIPT, Collections.singletonList(bloomUserNoteCollectListKey), noteId);
        NoteUnCollectLuaResultEnum noteUnCollectLuaResultEnum = NoteUnCollectLuaResultEnum.valueOf(luaResult);
        if (ObjUtil.isNull(noteUnCollectLuaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        switch (noteUnCollectLuaResultEnum) {
            //如果Bloom不存在
            case NOT_EXISTS: {
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                //异步同步Bloom
                threadPoolTaskExecutor.submit(() -> batchAddNoteCollectToBloom(userId, bloomUserNoteCollectListKey, expireSeconds));
                //搜索redis
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);
                //存在，则跳过
                if (ObjUtil.isNotNull(score)) {
                    break;
                }
                //select DB
                int count = noteCollectionDOMapper.selectCollectCountByUserIdAndNoteId(userId, noteId);
                // 不存在 ,抛出异常
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_UNCOLLECTED);
                }
                break;
            }
            //如果不存在
            case NOTE_NOT_COLLECT: {
                //抛出异常
                throw new BizException(ResponseCodeEnum.NOTE_UNCOLLECTED);
            }
        }
//       更新zset(允许误判)
        redisTemplate.opsForZSet().remove(userNoteCollectZSetKey, noteId);
//        发送MQ落库
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectTypeEnum.UNCOLLECTED.getCode())
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_COLLECT_OR_UNCOLLECTED + ":" + MQConstants.TAG_UNCOLLECTED;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("##发送[取消收藏笔记]消息落库成功，sendResult:{}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("##发送[取消收藏笔记]消息落库失败，throwable:{}", throwable);
            }
        });
        return Response.success();
    }

    private Long checkNoteIsExistAndGetCreatorId(Long noteId) {
        FindNoteDetailRspVO noteDetailLocalCache = LOCAL_NOTE_DETAIL_CACHE.getIfPresent(noteId);
        if (ObjUtil.isNotNull(noteDetailLocalCache)) {
            return noteDetailLocalCache.getCreatorId();
        }

//        构建RedisKey
        String noteDetailCacheKey = RedisKeyConstants.buildNoteDetailCacheKey(noteId);
//        查询Redis缓存
        String noteDetailRedisCache = (String) redisTemplate.opsForValue().get(noteDetailCacheKey);
//        存在，将数据存入本地缓存
        if (StringUtils.isNotBlank(noteDetailRedisCache)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseJSONString(noteDetailRedisCache, FindNoteDetailRspVO.class);
            if (ObjUtil.isNull(findNoteDetailRspVO)) {
                throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
            }
            threadPoolTaskExecutor.submit(() -> LOCAL_NOTE_DETAIL_CACHE.put(noteId, findNoteDetailRspVO));
            return findNoteDetailRspVO.getCreatorId();
        }
//        如果不存在则查询DB
        Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);
//        DB不存在，则抛出错误
        if (ObjUtil.isNull(creatorId)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        }
//        DB存在该笔记，则异步落库
        threadPoolTaskExecutor.submit(() -> {
            FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder().id(noteId).build();
            findNoteDetail(findNoteDetailReqVO);
        });

        return creatorId;
    }

    private void batchAddNoteCollectDBToZSet(Long userId, String noteCollectZSetKey, long expireSeconds) {
        batchAddNoteCollectToZSet(userId, noteCollectZSetKey, expireSeconds, null, null);
    }

    private void batchAddNoteCollectDBAndArgsToZSet(Long userId, String noteCollectZSetKey, long expireSeconds, Long argsNoteId, LocalDateTime createTime) {
        batchAddNoteCollectToZSet(userId, noteCollectZSetKey, expireSeconds, argsNoteId, createTime);
    }

    private void batchAddNoteCollectToZSet(Long userId, String noteCollectZSetKey, long expireSeconds, Long argsNoteId, LocalDateTime createTime) {
        Boolean hasKey = redisTemplate.hasKey(noteCollectZSetKey);
        if (!hasKey) {
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectByNoteIdAndLimit(userId, 300L);

            if (ObjUtil.isNotNull(argsNoteId)) {
                NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                        .noteId(argsNoteId)
                        .createTime(createTime)
                        .build();
                if (noteCollectionDOS.size() >= 300) {
                    noteCollectionDOS.set(noteCollectionDOS.size() - 1, noteCollectionDO);
                } else {
                    noteCollectionDOS.add(noteCollectionDO);
                }
            }
            if (CollUtil.isEmpty(noteCollectionDOS)) {
                return;
            }

            Object[] args = buildBatchAddNoteCollectZsetArgs(noteCollectionDOS, expireSeconds);
            redisTemplate.execute(NOTE_COLLECT_BATCH_ADD_AND_EXPIRE_SZET_SCRIPT, Collections.singletonList(noteCollectZSetKey), args);
        }
    }

    private Object[] buildBatchAddNoteCollectZsetArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int size = noteCollectionDOS.size() * 2 + 1;
        Object[] args = new Object[size];
        int index = 0;

        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            args[index] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime());
            args[index + 1] = noteCollectionDO.getNoteId();
            index += 2;
        }

        args[size - 1] = expireSeconds;

        return args;
    }

    private void batchAddNoteCollectToBloom(Long userId, String key, long expireSeconds) {

        List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

        if (CollUtil.isEmpty(noteCollectionDOS)) {
            return;
        }

        List<Object> args = noteCollectionDOS.stream().map(noteCollectionDO ->
                        (Object) noteCollectionDO.getNoteId())
                .collect(Collectors.toList());
        args.add(expireSeconds);

        redisTemplate.execute(BLOOM_BATCH_ADD_NOTE_COLLECT_AND_EXPIRE_SCRIPT, Collections.singletonList(key), args.toArray());
    }

    private void asyncBatchAddDBNoteLike2Bloom(Long userId, long expireTime, String bloomUserNoteLikeRedisKey) {
        asyncBatchAddNoteLike2Bloom(userId, expireTime, bloomUserNoteLikeRedisKey, null);
    }

    private void asyncBatchAddDBAndArgsNoteLike2Bloom(Long userId, long expireTime, String
            bloomUserNoteLikeRedisKey, Long argsNoteId) {
        asyncBatchAddNoteLike2Bloom(userId, expireTime, bloomUserNoteLikeRedisKey, argsNoteId);
    }

    private void asyncBatchAddNoteLike2Bloom(Long userId, long expireTime, String bloomUserNoteLikeRedisKey, Long
            argsNoteId) {
        threadPoolTaskExecutor.submit(() -> {
//            查询DB该用户下的所有数据
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);
            List<Object> args = new ArrayList<>();
//            构建参数列表
            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                args = noteLikeDOS.stream()
                        .map(noteLikeDO -> (Object) noteLikeDO.getNoteId())
                        .collect(Collectors.toList());
            }
            if (ObjUtil.isNotNull(argsNoteId)) {
                args.add(argsNoteId);
            }
            if (CollUtil.isEmpty(args)) {
                return;
            }
            args.add(expireTime);
//            执行RedisScript
            redisTemplate.execute(BLOOM_BATCH_ADD_NOTE_LIKE_AND_EXPIRE_SCRIPT, Collections.singletonList(bloomUserNoteLikeRedisKey), args.toArray());
        });
    }

    public void batchAddNoteLikeZSet(Long userId) {
        initUserNoteLikesZSet(userId, null, null);
    }

    public void batchAddNoteLikeAndArgsNoteIdToZset(Long userId, Long argsNoteId, LocalDateTime createTime) {
        initUserNoteLikesZSet(userId, argsNoteId, createTime);
    }

    public void initUserNoteLikesZSet(Long userId, Long argsNoteId, LocalDateTime createTime) {
//        构建key和过期时间
        String noteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
//        查询数据库
        List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100L);
//        如果argsNoteId不为空
        if (ObjUtil.isNotNull(argsNoteId)) {
            NoteLikeDO addNewNoteLikeDO = NoteLikeDO.builder()
                    .noteId(argsNoteId)
                    .createTime(createTime)
                    .build();
//        如果noteLikeDOS的大小是超过了100，则替换掉最后一条数据
            if (noteLikeDOS.size() >= 100) {
                noteLikeDOS.set(noteLikeDOS.size() - 1, addNewNoteLikeDO);
            } else {
//        如果没有，则直接添加即可
                noteLikeDOS.add(addNewNoteLikeDO);
            }
        }
//        如果noteLikeDOS为空，则直接返回
        if (CollUtil.isEmpty(noteLikeDOS)) {
            return;
        }
//        构建参数列表
        Object[] args = buildBatchAddNoteLikeZsetArgs(noteLikeDOS, expireSeconds);
//        执行对应的script
        redisTemplate.execute(NOTE_LIKE_BATCH_ADD_AND_EXPIRE_ZSET_SCRIPT, Collections.singletonList(noteLikeZSetKey), args);
    }

    private Object[] buildBatchAddNoteLikeZsetArgs(List<NoteLikeDO> noteLikeDOS, long expireSeconds) {
        int size = noteLikeDOS.size();
        Object[] args = new Object[2 * size + 1];
        int index = 0;
        for (NoteLikeDO noteLikeDO : noteLikeDOS) {
            args[index] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime());
            args[index + 1] = noteLikeDO.getNoteId();

            index += 2;
        }

        args[args.length - 1] = expireSeconds;
        return args;
    }


}