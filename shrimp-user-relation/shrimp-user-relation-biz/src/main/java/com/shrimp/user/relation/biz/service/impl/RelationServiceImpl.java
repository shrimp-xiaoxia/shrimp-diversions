package com.shrimp.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.framework.common.util.ListOrderedUtils;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.user.relation.biz.constants.MQConstants;
import com.shrimp.user.relation.biz.constants.RedisKeyConstants;
import com.shrimp.user.relation.biz.domain.dataobject.FansDO;
import com.shrimp.user.relation.biz.domain.dataobject.FollowingDO;
import com.shrimp.user.relation.biz.domain.mapper.FansDOMapper;
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.biz.enums.LuaFollowResultEnum;
import com.shrimp.user.relation.biz.enums.LuaUnFollowResultEnum;
import com.shrimp.user.relation.biz.enums.ResponseCodeEnum;
import com.shrimp.user.relation.biz.model.dto.FollowUserMQDTO;
import com.shrimp.user.relation.biz.model.dto.UnFollowUserMQDTO;
import com.shrimp.user.relation.biz.model.vo.*;
import com.shrimp.user.relation.biz.rpc.UserRpcService;
import com.shrimp.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.units.qual.K;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserRpcService userRpcService;
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final static DefaultRedisScript<Long> FOLLOW_CHECK_AND_ADD_SCRIPT;

    static {
        FOLLOW_CHECK_AND_ADD_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua"));
        FOLLOW_CHECK_AND_ADD_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_CHECK_AND_ADD_SCRIPT.setResultType(Long.class);
    }

    private final static DefaultRedisScript<Long> FOLLOW_ADD_AND_EXPIRE_SCRIPT;

    static {
        FOLLOW_ADD_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua"));
        FOLLOW_ADD_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_ADD_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private final static DefaultRedisScript<Long> FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT;

    static {
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua"));
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT.setScriptSource(resourceScriptSource);
        FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private final static DefaultRedisScript<Long> UNFOLLOW_CHECK_AND_REMOVE_SCRIPT;

    static {
        UNFOLLOW_CHECK_AND_REMOVE_SCRIPT = new DefaultRedisScript<>();
        ResourceScriptSource resourceScriptSource = new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_remove.lua"));
        UNFOLLOW_CHECK_AND_REMOVE_SCRIPT.setScriptSource(resourceScriptSource);
        UNFOLLOW_CHECK_AND_REMOVE_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private FansDOMapper fansDOMapper;

    @Override
    public Response<?> followUser(FollowUserReqVO followUserReqVO) {
//          获取关注用户ID
        Long followUserId = followUserReqVO.getFollowUserId();
//          获取当前用户ID
        Long loginUserId = LoginUserContextHolder.getLoginUserId();
//          若关注的用户是自己，则抛出异常
        if (ObjUtil.equals(loginUserId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }
//          校验关注的用户是否存在
        boolean flag = userRpcService.checkUserIsExist(followUserId);
        if (!flag) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

//          执行redis的Lua脚本
        String key = RedisKeyConstants.buildFollowUserZsetKey(loginUserId);
        LocalDateTime createTime = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(createTime);

        Long result = redisTemplate.execute(FOLLOW_CHECK_AND_ADD_SCRIPT, Collections.singletonList(key), followUserId, timestamp);

        checkFollowLuaScriptResult(result);

        if (LuaFollowResultEnum.valueOf(result).equals(LuaFollowResultEnum.ZSET_NOT_EXIST)) {
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(loginUserId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            if (followingDOS == null || followingDOS.isEmpty()) {
                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策略
                redisTemplate.execute(FOLLOW_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), followUserId, timestamp, expireSeconds);
            } else {
                Object[] objects = buildFollowArgs(followingDOS, expireSeconds);
                redisTemplate.execute(FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), objects);

                result = redisTemplate.execute(FOLLOW_CHECK_AND_ADD_SCRIPT, Collections.singletonList(key), followUserId, timestamp);
                checkFollowLuaScriptResult(result);
            }
        }
        // 发送 MQ
//        构建消息对象
        FollowUserMQDTO followUserMQDTO = FollowUserMQDTO.builder()
                .userId(loginUserId)
                .followUserId(followUserId)
                .createTime(createTime)
                .build();
//        转换为JSON格式
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMQDTO)).build();
// 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;
        log.info("==> 开始发送关注操作 MQ, 消息体: {}", followUserMQDTO);
//        异步发送消息给消费端，提高接口响应速度
//        同一个操作用户需要顺序发送消息
        String mqKey = String.valueOf(loginUserId);
        rocketMQTemplate.asyncSendOrderly(destination, message, mqKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public Response<?> unfollowUser(UnFollowUserReqVO unFollowUserReqVO) {
//          获取取关用户ID
        Long unFollowUserId = unFollowUserReqVO.getUnfollowUserId();
//          获取当前用户ID
        Long loginUserId = LoginUserContextHolder.getLoginUserId();
//          若取关的用户是自己，则抛出异常
        if (ObjUtil.equals(loginUserId, unFollowUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }
//          校验关注的用户是否存在
        boolean flag = userRpcService.checkUserIsExist(unFollowUserId);
        if (!flag) {
            throw new BizException(ResponseCodeEnum.UNFOLLOW_USER_NOT_EXISTED);
        }
//        执行检查和删除lua脚本
        String key = RedisKeyConstants.buildFollowUserZsetKey(loginUserId);
        Long result = redisTemplate.execute(UNFOLLOW_CHECK_AND_REMOVE_SCRIPT, Collections.singletonList(key), unFollowUserId);
//        检查Lua结果
        checkUnFollowLuaScriptResult(result);
//        不存在zset
        if (LuaUnFollowResultEnum.valueOf(result).equals(LuaUnFollowResultEnum.ZSET_NOT_EXIST)) {
            //查询数据库
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(loginUserId);
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            //若查询出来的List为空，则抛出取关用户不存在的异常
            if (followingDOS == null || followingDOS.isEmpty()) {
                throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
            }
            //批量插入，执行lua脚本
            Object[] args = buildFollowArgs(followingDOS, expireSeconds);
            redisTemplate.execute(FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), args);
            //再次执行检查和删除lua脚本
            result = redisTemplate.execute(UNFOLLOW_CHECK_AND_REMOVE_SCRIPT, Collections.singletonList(key), unFollowUserId);
            //检查Lua结果
            checkUnFollowLuaScriptResult(result);
        }
//        构建message
        UnFollowUserMQDTO unFollowUserMQDTO = UnFollowUserMQDTO.builder()
                .userId(loginUserId)
                .unFollowUserId(unFollowUserId)
                .createTime(LocalDateTime.now())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(unFollowUserMQDTO)).build();
//        构建topic和tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;
        log.info("==> 开始发送取关操作 MQ, 消息体: {}", unFollowUserMQDTO);
//        异步发送MQ
//        同一个操作用户需要顺序发送消息
        String mqKey = String.valueOf(loginUserId);
        rocketMQTemplate.asyncSendOrderly(destination, message, mqKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
//         检查参数，如当前页数不能为负数
        if (findFollowingListReqVO.getPageNo() <= 0) {
            findFollowingListReqVO.setPageNo(1);
        }
//        提取用户ID
        Long userId = findFollowingListReqVO.getUserId();
        long pageNo = findFollowingListReqVO.getPageNo();
        long pageSize = 10;
//        构建RedisKey
        String userFollowKey = RedisKeyConstants.buildFollowUserZsetKey(userId);
//        创建返参
        List<FindFollowingUserRspVO> findFollowingUserRspVOList = null;
//        查询key的数据量
        Long totalCount = redisTemplate.opsForZSet().zCard(userFollowKey);
//        如果key存在
        if (totalCount != null && totalCount > 0) {
//        默认一页数据量为10，计算offset和limit

            long offset = (pageNo - 1) * pageSize;
//            判断是否越界
            long totalPage = PageResponse.getTotalPage(totalCount, pageSize);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, totalCount);
            }
//        使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
//        注意：这里使用了 Double.POSITIVE_INFINITY 和 Double.NEGATIVE_INFINITY 作为分数范围
//        因为关注列表最多有 1000 个元素，这样可以确保获取到所有的元素
//        redis获取到数据(注意顺序)
            Set<Object> followingUserSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(userFollowKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize);
//        不为空，则发起远程调用
            if (CollUtil.isEmpty(followingUserSet)) {
                return PageResponse.success(null, pageNo, totalCount);
            }
            List<Long> userIds = followingUserSet.stream().map(obj -> Long.valueOf(String.valueOf(obj))).toList();

            findFollowingUserRspVOList = rpcGetFindFollowingUserRspVO(userIds, findFollowingUserRspVOList);
        } else {
//         如果key不存在

//         查询DB数据库中的该用户关注总数
            totalCount = followingDOMapper.selectFollowCountByUserId(userId);
//          计算offset和limit
            long offset = PageResponse.getOffset(pageNo, pageSize);
//            判断是否越界
            long totalPage = PageResponse.getTotalPage(totalCount, pageSize);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, totalCount);
            }
//        去到DB数据库查询数据(倒序排序，只用查询出用户ID)
            List<FollowingDO> followingDOList = followingDOMapper.selectPageListByUserId(userId, offset, pageSize);
//        不为空，则发起远程调用
            if (CollUtil.isEmpty(followingDOList)) {
                return PageResponse.success(null, pageNo, totalCount);
            }
            List<Long> userIds = followingDOList.stream().map(FollowingDO::getFollowingUserId).toList();
            findFollowingUserRspVOList = rpcGetFindFollowingUserRspVO(userIds, findFollowingUserRspVOList);
//        异步调用将DB数据同步到RedisCache中
            threadPoolTaskExecutor.submit(() -> syncFollowingList2Redis(userId));
        }
//        返回结果
        return PageResponse.success(findFollowingUserRspVOList, pageNo, totalCount);
    }

    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO) {
//        同findFollowingList方法逻辑
        if (findFansListReqVO.getPageNo() <= 0) {
            findFansListReqVO.setPageNo(1);
        }
        Long userId = findFansListReqVO.getUserId();
        long pageNo = findFansListReqVO.getPageNo();
        long pageSize = 10;
        String fansZsetKey = RedisKeyConstants.buildUserFansZsetKey(userId);
        Long totalCount = redisTemplate.opsForZSet().zCard(fansZsetKey);
        List<FindFansUserRspVO> findFansListRspVOUser = new ArrayList<>();

        if (totalCount != null && totalCount > 0) {
            long offset = PageResponse.getOffset(pageNo, pageSize);
            long totalPage = PageResponse.getTotalPage(totalCount, pageSize);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, totalCount);
            }

            Set<Object> fansRedisSet = redisTemplate.opsForZSet().reverseRangeByScore(fansZsetKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize);
            if (CollUtil.isEmpty(fansRedisSet)) {
                return PageResponse.success(null, pageNo, totalCount);
            }
            List<Long> userIds = fansRedisSet.stream().map(obj -> Long.valueOf(String.valueOf(obj))).toList();
            findFansListRspVOUser = rpcGetFindFansUserRspVO(userIds, findFansListRspVOUser);
        } else {
            totalCount = fansDOMapper.selectFansCountByUserId(userId);
            long offset = PageResponse.getOffset(pageNo, pageSize);
            long totalPage = PageResponse.getTotalPage(totalCount, pageSize);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, totalCount);
            }

            List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, pageSize);
            if (CollUtil.isEmpty(fansDOS)) {
                return PageResponse.success(null, pageNo, totalCount);
            }

            List<Long> fansUserId = fansDOS.stream().map(FansDO::getFansUserId).toList();
            findFansListRspVOUser = rpcGetFindFansUserRspVO(fansUserId, findFansListRspVOUser);

            threadPoolTaskExecutor.submit(() -> syncFansList2Redis(userId));
        }
        return PageResponse.success(findFansListRspVOUser, pageNo, totalCount);
    }

    private void syncFollowingList2Redis(Long userId) {
        //查出该用户关注的所有数据
        List<FollowingDO> followingDOS = followingDOMapper.selectAllFollowingByUserId(userId);
        if (CollUtil.isEmpty(followingDOS)) {
            return;
        }
//            构建key和TTL
        String key = RedisKeyConstants.buildFollowUserZsetKey(userId);
        long expireSecond = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        //构建参数， 使用lua脚本
        Object[] args = buildFollowArgs(followingDOS, expireSecond);

        redisTemplate.execute(FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), args);
    }

    private void syncFansList2Redis(Long userId) {
        //查出该用户关注的所有数据
        List<FansDO> fansList = fansDOMapper.selectMax5000ByUserId(userId);
        if (CollUtil.isEmpty(fansList)) {
            return;
        }
//            构建key和TTL
        String key = RedisKeyConstants.buildUserFansZsetKey(userId);
        long expireSecond = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        //构建参数， 使用lua脚本
        Object[] args = buildFansArgs(fansList, expireSecond);
        redisTemplate.execute(FOLLOW_BATCH_ADD_AND_EXPIRE_SCRIPT, Collections.singletonList(key), args);
    }

    private List<FindFollowingUserRspVO> rpcGetFindFollowingUserRspVO(List<Long> userIds, List<FindFollowingUserRspVO> findFollowingUserRspVOList) {
        List<FindUserByIdRspDTO> findUserByIdRspDTOList = userRpcService.findUserByIds(userIds);
//        如果调用的结果为空则直接返回
        if (CollUtil.isNotEmpty(findUserByIdRspDTOList)) {
            //        DTO装换成FindFollowingUserRespVO
            findFollowingUserRspVOList = findUserByIdRspDTOList.stream().map(dto ->
                    FindFollowingUserRspVO.builder()
                            .userId(dto.getId())
                            .nickname(dto.getNickName())
                            .avatar(dto.getAvatar())
                            .introduction(dto.getIntroduction())
                            .build()
            ).toList();
        }

        findFollowingUserRspVOList = ListOrderedUtils.sortListByOrderedIds(userIds, findFollowingUserRspVOList, FindFollowingUserRspVO::getUserId);
        return findFollowingUserRspVOList;
    }


    private List<FindFansUserRspVO> rpcGetFindFansUserRspVO(List<Long> userIds, List<FindFansUserRspVO> findFansUserRspVOList) {
        List<FindUserByIdRspDTO> findUserByIdRspDTOList = userRpcService.findUserByIds(userIds);
        if (CollUtil.isEmpty(findUserByIdRspDTOList)) {
            return findFansUserRspVOList;
        }
        // TODO RPC: 批量查询用户的计数数据（笔记总数、粉丝总数）
        findFansUserRspVOList = findUserByIdRspDTOList.stream().map(dto ->
                        FindFansUserRspVO.builder()
                                .userId(dto.getId())
                                .avatar(dto.getAvatar())
                                .nickName(dto.getNickName())
//                    后续再补充
                                .fansTotal(0L)
                                .noteTotal(0L)
                                .build()
        ).toList();

        findFansUserRspVOList = ListOrderedUtils.sortListByOrderedIds(userIds, findFansUserRspVOList, FindFansUserRspVO::getUserId);
        return findFansUserRspVOList;
    }

    private void checkFollowLuaScriptResult(Long luaResult) {
        LuaFollowResultEnum luaFollowResultEnum = LuaFollowResultEnum.valueOf(luaResult);
        if (Objects.isNull(luaFollowResultEnum)) {
            throw new RuntimeException("Lua 返回结果错误");
        }

        switch (luaFollowResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    private void checkUnFollowLuaScriptResult(Long luaResult) {
        LuaUnFollowResultEnum luaUnFollowResultEnum = LuaUnFollowResultEnum.valueOf(luaResult);
        if (Objects.isNull(luaUnFollowResultEnum)) {
            throw new RuntimeException("Lua 返回结果错误");
        }

        if (luaUnFollowResultEnum.equals(LuaUnFollowResultEnum.USER_NOT_EXISTED)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOLLOW_FAIL);
        }
    }

    private Object[] buildFollowArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int listSize = followingDOS.size();
        Object[] args = new Object[2 * listSize + 1];
        int index = 0;
        for (FollowingDO followingDO : followingDOS) {
            args[index] = DateUtils.localDateTime2Timestamp(followingDO.getCreateTime());
            args[index + 1] = followingDO.getFollowingUserId();
            index += 2;
        }
        args[args.length - 1] = expireSeconds;
        return args;
    }

    private Object[] buildFansArgs(List<FansDO> fansDOS, long expireSeconds) {
        int listSize = fansDOS.size();
        Object[] args = new Object[2 * listSize + 1];
        int index = 0;
        for (FansDO fansDO : fansDOS) {
            args[index] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime());
            args[index + 1] = fansDO.getFansUserId();
            index += 2;
        }
        args[args.length - 1] = expireSeconds;
        return args;
    }
}
