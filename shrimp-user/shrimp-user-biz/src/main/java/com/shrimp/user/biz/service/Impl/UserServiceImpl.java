package com.shrimp.user.biz.service.Impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
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
import com.shrimp.user.dto.req.FindUserByIdReqDTO;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        if (StringUtils.isNotBlank(introduction)) {
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
            userDOMapper.updateByPrimaryKeySelective(userDO);
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
                .nickname("小红薯" + xiaohashuId)
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
        if(ObjUtil.isNotNull(findUserByIdRspDTOLocalCache)) {
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
            if(ObjUtil.isNotNull(findUserByIdRspDTO)) {
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
                .nickName(userDO.getNickname()).build();
//        异步缓存该值
        threadPoolTaskExecutor.submit(() -> {
            int ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 24 * 24);
            redisTemplate.opsForValue().set(userCancelKey, JsonUtils.toJsonString(findUserByIdRspDTO), ttl, TimeUnit.SECONDS);
        });
//        返回结果
        return  Response.success(findUserByIdRspDTO);
    }
}
