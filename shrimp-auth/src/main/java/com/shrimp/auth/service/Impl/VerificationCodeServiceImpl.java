package com.shrimp.auth.service.Impl;

import cn.hutool.core.util.RandomUtil;
import com.shrimp.auth.constants.RedisKeyConstants;
import com.shrimp.auth.enums.ResponseCodeEnum;
import com.shrimp.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.shrimp.auth.service.VerificationCodeService;
import com.shrimp.auth.sms.AliyunSmsHepler;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private AliyunSmsHepler aliyunSmsHepler;
    @Override
    public Response<?> generateVerificationCode(SendVerificationCodeReqVO vo) {
        String phone = vo.getPhone();
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        Boolean hasKey = redisTemplate.hasKey(key);
        if(hasKey){
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        String verificationCode = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().setIfAbsent(key,verificationCode,3, TimeUnit.MINUTES);
//        调用第三方平台发送短信消息
        log.info("==> 手机号: {}, 已生成验证码：【{}】", phone, verificationCode);
//        TODO 有需要再用
//        taskExecutor.submit(()->{
//            String signName = "速通互联验证码"; // 签名，个人测试签名无法修改
//            String templateCode = "100001";
//            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"3\"}", verificationCode);
//            aliyunSmsHepler.sendMessage(signName,templateCode,phone,templateParam);
//        });

        return Response.success();
    }
}
