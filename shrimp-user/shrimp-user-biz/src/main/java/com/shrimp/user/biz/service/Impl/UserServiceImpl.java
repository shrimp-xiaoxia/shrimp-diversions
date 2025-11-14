package com.shrimp.user.biz.service.Impl;

import cn.hutool.core.util.ObjUtil;
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
import com.shrimp.user.biz.rpc.OssRpcService;
import com.shrimp.user.biz.service.UserService;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        Long xiaohashuId = redisTemplate.opsForValue().increment(RedisKeyConstants.SHRIMP_ID_GENERATOR_KEY);
//        创建UserDO
        UserDO userDO = UserDO.builder()
                .xiaohashuId(String.valueOf(xiaohashuId))
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

        if(ObjUtil.isNull(userDO)){
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
}
