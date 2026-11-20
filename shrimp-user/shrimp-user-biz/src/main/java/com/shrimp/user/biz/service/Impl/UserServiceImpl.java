package com.shrimp.user.biz.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.JsonUtils;
import com.shrimp.framework.common.util.ParamUtils;
import com.shrimp.user.biz.constants.RedisKeyConstants;
import com.shrimp.user.biz.constants.RoleConstants;
import com.shrimp.user.biz.domain.dataobject.RoleDO;
import com.shrimp.user.biz.domain.dataobject.UserDO;
import com.shrimp.user.biz.domain.dataobject.UserRoleDO;
import com.shrimp.user.biz.domain.mapper.RoleDOMapper;
import com.shrimp.user.biz.domain.mapper.UserDOMapper;
import com.shrimp.user.biz.domain.mapper.UserRoleDOMapper;
import com.shrimp.user.biz.enums.DeletedEnum;
import com.shrimp.user.biz.enums.ResponseCodeEnum;
import com.shrimp.user.biz.enums.SexEnum;
import com.shrimp.user.biz.enums.StausEnum;
import com.shrimp.user.biz.model.vo.UpdateUserInfoReqVO;
import com.shrimp.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.shrimp.user.biz.rpc.OssRpcService;
import com.shrimp.user.biz.service.UserService;
import com.shrimp.user.dto.req.*;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private UserDOMapper userDOMapper;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource(name = "taskThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 用户信息本地缓存
     */
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        userDO.setId(LoginUserContextHolder.getLoginUserId());
        boolean updateFlag = false;
//        头像
        MultipartFile avatar = updateUserInfoReqVO.getAvatar();

        if (avatar != null && avatar.getSize() > 0) {
            String avatarUrl = ossRpcService.uploadFile(avatar);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", avatarUrl);
//            TODO 调用对象存储服务接口
            if (StringUtils.isBlank(avatarUrl)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }
            userDO.setAvatar(avatarUrl);
            updateFlag = true;
        }

        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (birthday != null) {
            Preconditions.checkArgument(!birthday.isAfter(LocalDate.now()), "生日不能是未来日期");
            userDO.setBirthday(birthday);
            updateFlag = true;
        }

        String nickName = updateUserInfoReqVO.getNickName();
        if (StringUtils.isNotBlank(nickName)) {
            Preconditions.checkArgument(ParamUtils.checkNickName(nickName), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickName);
            updateFlag = true;
        }

        String xiaohashuId = updateUserInfoReqVO.getXiaohashuId();
        if (StringUtils.isNotBlank(xiaohashuId)) {
            Preconditions.checkArgument(ParamUtils.checkXiaohashuId(xiaohashuId), ResponseCodeEnum.XIAOHASHU_ID_VALID_FAIL.getErrorMessage());
            userDO.setXiaohashuId(xiaohashuId);
            updateFlag = true;
        }

        Integer sex = updateUserInfoReqVO.getSex();
        if (ObjUtil.isNotNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(Byte.parseByte(sex.toString()));
            updateFlag = true;
        }

        String introduction = updateUserInfoReqVO.getIntroduction();
        if (introduction != null) {
            Preconditions.checkArgument(ParamUtils.checkStrLength(introduction, 100), ResponseCodeEnum.INTRODUCE_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            updateFlag = true;
        }

        MultipartFile backgroundImg = updateUserInfoReqVO.getBackgroundImg();
        if (backgroundImg != null && backgroundImg.getSize() > 0) {
//              TODO 调用对象存储服务接口
            String backGroundImagUrl = ossRpcService.uploadFile(backgroundImg);
            log.info("==> 调用 oss 服务成功，上传背景图，url：{}", backGroundImagUrl);
            if (StringUtils.isBlank(backGroundImagUrl)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }
            userDO.setBackgroundImg(backGroundImagUrl);
            updateFlag = true;
        }

        if (updateFlag) {
            userDO.setUpdateTime(LocalDateTime.now());
            int updated = userDOMapper.updateByPrimaryKeySelective(userDO);
            if (updated > 0) {
                LOCAL_CACHE.invalidate(userDO.getId());
                redisTemplate.delete(RedisKeyConstants.buildUserInfoCancelKey(userDO.getId()));
            }
        }

        return Response.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Response<Long> registerUser(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();
        Long userId = null;

        UserDO user = userDOMapper.selectByPhone(phone);
        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(user));
        if (ObjUtil.isNotNull(user)) {
            userId = user.getId();
            return Response.success(userId);
        }

//    获取全局小红书id
        String xiaohashuId = distributedIdGeneratorRpcService.getXiaohashuId();

//        获取用户ID
        String id = distributedIdGeneratorRpcService.getUserId();
//        创建UserDO
        UserDO userDO = UserDO.builder()
                .id(Long.valueOf(id))
                .xiaohashuId(xiaohashuId)
                .nickname("虾趣用户" + xiaohashuId)
                .phone(phone)
                .status(StausEnum.ENABLE.getValue())
                .isDeleted(DeletedEnum.NO.getValue())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

//        插入userDO
        userDOMapper.insert(userDO);
//        获取userId
        userId = userDO.getId();
//        创建UserRoleDO
        UserRoleDO userRoleDO = UserRoleDO.builder()
                .userId(userId)
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
//        插入userRoleDo
        userRoleDOMapper.insert(userRoleDO);
///       获取该用户分配的角色id
        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);
        List<String> rolesKey = new ArrayList<>(1);
        rolesKey.add(roleDO.getRoleKey());
        String userRoleKey = RedisKeyConstants.buildUserRoleKey(userId);
//        插入到Redis中，为后续鉴权做准备
        redisTemplate.opsForValue().set(userRoleKey, JsonUtils.toJsonString(rolesKey));

        return Response.success(userId);
    }

    @Override
    public Response<FindUserByPhoneRspDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        UserDO userDO = userDOMapper.selectByPhone(findUserByPhoneReqDTO.getPhone());

        if (ObjUtil.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserByPhoneRspDTO dto = FindUserByPhoneRspDTO.builder()
                .userId(userDO.getId())
                .password(userDO.getPassword())
                .build();

        return Response.success(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        Long userId = LoginUserContextHolder.getLoginUserId();
        String encodePassword = updateUserPasswordReqDTO.getEncodePassword();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .updateTime(LocalDateTime.now())
                .password(encodePassword)
                .build();

        userDOMapper.updateByPrimaryKeySelective(userDO);
        return Response.success();
    }

    @Override
    public Response<FindUserByIdRspDTO> findUserById(FindUserByIdReqDTO findUserByIdReqDTO) {
//        提取ID
        Long id = findUserByIdReqDTO.getId();
//        本地缓存
        FindUserByIdRspDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(id);
        if (ObjUtil.isNotNull(findUserByIdRspDTOLocalCache)) {
            log.info("==> 命中了本地缓存；{}", findUserByIdRspDTOLocalCache);
            return Response.success(findUserByIdRspDTOLocalCache);
        }

//        构建key寻找Redis缓存
        String userCancelKey = RedisKeyConstants.buildUserInfoCancelKey(id);
        String JSONStr = (String) redisTemplate.opsForValue().get(userCancelKey);
//        JSONStr不为空，则返回其值
        if (StringUtils.isNotBlank(JSONStr)) {
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseJSONString(JSONStr, FindUserByIdRspDTO.class);
//            添加本地缓存
            if (ObjUtil.isNotNull(findUserByIdRspDTO)) {
                threadPoolTaskExecutor.submit(() -> {
                    LOCAL_CACHE.put(id, findUserByIdRspDTO);
                });
            }
            return Response.success(findUserByIdRspDTO);
        }
//        若为空，则DB查询数据
        UserDO userDO = userDOMapper.selectById(id);
//        DB中的数据不存在该ID
        if (ObjUtil.isNull(userDO)) {
            //        异步缓存null
            threadPoolTaskExecutor.execute(() -> {
                int ttl = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userCancelKey, "null", ttl, TimeUnit.SECONDS);
            });
            //        抛出错误
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

//        DB中的数据存在该ID
        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO
                .builder()
                .id(userDO.getId())
                .avatar(userDO.getAvatar())
                .nickName(userDO.getNickname())
                .introduction(userDO.getIntroduction())
                .build();
//        异步缓存该值
        threadPoolTaskExecutor.submit(() -> {
            int ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 24 * 24);
            redisTemplate.opsForValue().set(userCancelKey, JsonUtils.toJsonString(findUserByIdRspDTO), ttl, TimeUnit.SECONDS);
        });
//        返回结果
        return Response.success(findUserByIdRspDTO);
    }

    @Override
    public Response<List<FindUserByIdRspDTO>> findUserByIds(FindUserByIdsReqDTO findUserByIdsReqDTO) {
//        提取IDS
        List<Long> ids = findUserByIdsReqDTO.getIds();
//        将ID集合转换成redisKey
        List<String> redisKeys = ids.stream().map(RedisKeyConstants::buildUserInfoCancelKey).toList();
//        发起redis批量查询
        List<Object> redisUserInfos = redisTemplate.opsForValue().multiGet(redisKeys);
//        过滤掉为null的结果
        if (CollUtil.isNotEmpty(redisUserInfos)) {
            redisUserInfos = redisUserInfos.stream().filter(ObjUtil::isNotNull).toList();
        }
//        创建List集合接收数据，作为返参
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();
//        将Redis结果装换
        if (CollUtil.isNotEmpty(redisUserInfos)) {
            findUserByIdRspDTOS = new ArrayList<>(redisUserInfos.stream().map(info ->
                    JsonUtils.parseJSONString(String.valueOf(info), FindUserByIdRspDTO.class)
            ).toList());
        }
//        如果要查询的数据都在Redis缓存中，直接返回数据
        if (findUserByIdRspDTOS.size() == ids.size()) {
            return Response.success(findUserByIdRspDTOS);
        }
//        还有另外两种情况：一种是缓存里没有用户信息数据，还有一种是缓存里数据不全，需要从数据库中补充
//        筛选出缓存里没有的用户数据，去查数据库
        List<Long> userIdNeedQuery = null;
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
//            使用hash法，将时间复杂度n^2降为n
            Map<Long, FindUserByIdRspDTO> map = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));
            userIdNeedQuery = ids.stream().filter(id ->
                    !map.containsKey(id)
            ).toList();
        } else {
            userIdNeedQuery = ids;
        }
//        将数据库中的数据转换
        List<UserDO> userDOS = userDOMapper.selectByUserIds(userIdNeedQuery);
        List<FindUserByIdRspDTO> findUserByIdRspDTOS2 = null;
        if (CollUtil.isNotEmpty(userDOS)) {
            findUserByIdRspDTOS2 = userDOS.stream().map(userDO ->
                    FindUserByIdRspDTO.builder()
                            .id(userDO.getId())
                            .avatar(userDO.getAvatar())
                            .nickName(userDO.getNickname())
                            .introduction(userDO.getIntroduction())
                            .build()
            ).collect(Collectors.toList());
            // 异步线程将用户信息同步到 Redis 中
            List<FindUserByIdRspDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;

            threadPoolTaskExecutor.submit(() -> {
                Map<Long, FindUserByIdRspDTO> redisMap = finalFindUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public Object execute(RedisOperations operations) {
                        for (UserDO userDO : userDOS) {
                            Long userDOIdCache = userDO.getId();
                            FindUserByIdRspDTO findUserByIdRspDTOCache = redisMap.get(userDOIdCache);
                            String userInfoKey = RedisKeyConstants.buildUserInfoCancelKey(userDOIdCache);

                            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                            operations.opsForValue().set(userInfoKey, JsonUtils.toJsonString(findUserByIdRspDTOCache), expireSeconds, TimeUnit.SECONDS);
                        }
                        return null;
                    }
                });

            });
        }
//        将Redis中的结果集和DB结果集合并
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS2)) {
            findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
        }
//        返回结果
        return Response.success(findUserByIdRspDTOS);
    }
}
