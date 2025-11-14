package com.shrimp.auth.service.Impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.base.Preconditions;
import com.shrimp.auth.constants.RedisKeyConstants;
import com.shrimp.auth.enums.LoginTypeEnum;
import com.shrimp.auth.enums.ResponseCodeEnum;
import com.shrimp.auth.model.vo.user.UpdatePasswordReqVO;
import com.shrimp.auth.model.vo.user.UserLoginReqVO;
import com.shrimp.auth.rpc.UserRpcService;
import com.shrimp.auth.service.AuthService;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserRpcService userRpcService;

    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
//        获取登录类型
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);

        Long userId = null;
        if (loginTypeEnum == null) {
            return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "该登录类型异常");
        }
//        根据登录类型来选择对应的登录逻辑
        switch (loginTypeEnum) {
            //        验证码登录
            case VERIFICATION_CODE:
                //      获取参数验证码 ? 无->return
                String paraCode = userLoginReqVO.getCode();

                Preconditions.checkArgument(StringUtils.isNoneBlank(paraCode), "验证码不能为空");

                //       拼接可以key获取对应的Redis验证码
                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                String redisCode = (String) redisTemplate.opsForValue().get(key);
                //       判断验证码是否相同 ,不同->return
                if (!paraCode.equals(redisCode)) {
                    return Response.fail(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }
//                如果没有账号则注册
                Long userIdTemp = userRpcService.register(phone);
                if(ObjUtil.isNull(userIdTemp)){
                    throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                }
                userId = userIdTemp;
                break;
//        密码登录
            case PASSWORD:
                String password = userLoginReqVO.getPassword();

                Preconditions.checkArgument(StringUtils.isNoneBlank(password), "密码不可以为空");
                FindUserByPhoneReqDTO findUserByPhoneReqDTO = new FindUserByPhoneReqDTO(phone);

                FindUserByPhoneRspDTO findUserByPhoneRspDTO = userRpcService.findUserByPhone(findUserByPhoneReqDTO);
                if(ObjUtil.isNull(findUserByPhoneRspDTO)){
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }
                boolean matchesFlag = passwordEncoder.matches(password, findUserByPhoneRspDTO.getPassword());
                if (!matchesFlag) {
                    return Response.fail(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = findUserByPhoneRspDTO.getUserId();
                break;
//        意料之外的类型
            default:
//                TODO
                break;
        }

        if (userId == null) {
            return Response.fail(ResponseCodeEnum.SYSTEM_ERROR.getErrorCode(), "用户注册异常");
        }
//        进行so-token-login,返回token令牌
        StpUtil.login(userId);
        // 获取 Token 令牌
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        // 返回 Token 令牌
        return Response.success(tokenInfo.tokenValue);
    }


//    @Transactional(rollbackFor = {Exception.class})
//    @Override
//    public Long registerUser(String phone) {
////        获取全局小红书id
//        Long xiaohashuId = redisTemplate.opsForValue().increment(RedisKeyConstants.SHRIMP_ID_GENERATOR_KEY);
////        创建UserDO
//        UserDO userDO = UserDO.builder()
//                .xiaohashuId(String.valueOf(xiaohashuId))
//                .nickname("小红薯" + xiaohashuId)
//                .phone(phone)
//                .status(StausEnum.ENABLE.getValue())
//                .isDeleted(DeletedEnum.NO.getValue())
//                .createTime(LocalDateTime.now())
//                .updateTime(LocalDateTime.now())
//                .build();
//
////        插入userDO
//        userDOMapper.insert(userDO);
//
////        获取userId
//        Long userId = userDO.getId();
////        创建UserRoleDO
//        UserRoleDO userRoleDO = UserRoleDO.builder()
//                .userId(userId)
//                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
//                .createTime(LocalDateTime.now())
//                .updateTime(LocalDateTime.now())
//                .isDeleted(DeletedEnum.NO.getValue())
//                .build();
////        插入userRoleDo
//        userRoleDOMapper.insert(userRoleDO);
//
//
////       获取该用户分配的角色id
//        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);
//        List<String> rolesKey = new ArrayList<>(1);
//        rolesKey.add(roleDO.getRoleKey());
//        String userRoleKey = RedisKeyConstants.buildUserRoleKey(userId);
////        插入到Redis中，为后续鉴权做准备
//        redisTemplate.opsForValue().set(userRoleKey, JsonUtils.toJsonString(rolesKey));
//
//        return userId;
//    }

    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (userId == null) {
            return Response.fail(ResponseCodeEnum.AUTHORIZATION_ERROR);
        }
        StpUtil.logout(userId);
        return Response.success();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        String newPassword = updatePasswordReqVO.getNewPassword();
        String encodePassword = passwordEncoder.encode(newPassword);
        boolean isSuccess = userRpcService.updatePassword(encodePassword);
        if(!isSuccess){
            return Response.fail("修改密码失败");
        }
        return Response.success();
    }


//    public Long registerUser2(String phone) {
//        return transactionTemplate.execute(status -> {
//            try {
//                //        获取全局小红书id
//                Long xiaohashuId = redisTemplate.opsForValue().increment(RedisKeyConstants.SHRIMP_ID_GENERATOR_KEY);
////        创建UserDO
//                UserDO userDO = UserDO.builder()
//                        .xiaohashuId(String.valueOf(xiaohashuId))
//                        .nickname("小红薯" + xiaohashuId)
//                        .phone(phone)
//                        .status(StausEnum.ENABLE.getValue())
//                        .isDeleted(DeletedEnum.NO.getValue())
//                        .createTime(LocalDateTime.now())
//                        .updateTime(LocalDateTime.now())
//                        .build();
//
////        插入userDO
//                userDOMapper.insert(userDO);
////        获取userId
//                Long userId = userDO.getId();
////        创建UserRoleDO
//                UserRoleDO userRoleDO = UserRoleDO.builder()
//                        .userId(userId)
//                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
//                        .createTime(LocalDateTime.now())
//                        .updateTime(LocalDateTime.now())
//                        .isDeleted(DeletedEnum.NO.getValue())
//                        .build();
////        插入userRoleDo
//                userRoleDOMapper.insert(userRoleDO);
/////       获取该用户分配的角色id
//                RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);
//                List<String> rolesKey = new ArrayList<>(1);
//                rolesKey.add(roleDO.getRoleKey());
//                String userRoleKey = RedisKeyConstants.buildUserRoleKey(userId);
////        插入到Redis中，为后续鉴权做准备
//                redisTemplate.opsForValue().set(userRoleKey, JsonUtils.toJsonString(rolesKey));
//                return userId;
//            } catch (Exception e) {
//                status.setRollbackOnly(); // 标记事务为回滚
//                log.error("==>系统注册异常:", e);
//                return null;
//            }
//        });
//    }
}
