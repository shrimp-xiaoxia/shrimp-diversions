package com.shrimp.auth.sms;

import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.shrimp.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AliyunSmsHepler {

    @Resource
    private com.aliyun.dypnsapi20170525.Client client;

    public boolean sendMessage(String signName,String templateCode,String phone,String templateParam){
         com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                 .setSignName(signName)
                 .setTemplateCode(templateCode)
                 .setPhoneNumber(phone)
                 .setTemplateParam(templateParam);
         com.aliyun.teautil.models.RuntimeOptions runtimeOptions = new com.aliyun.teautil.models.RuntimeOptions();

        try {
            log.info("==> 开始短信发送, phone: {}, signName: {}, templateCode: {}, templateParam: {}", phone, signName, templateCode, templateParam);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(sendSmsVerifyCodeRequest, runtimeOptions);
            log.info("==> 短信发送成功, response: {}", JsonUtils.toJsonString(response));
            return true;
        } catch (Exception e) {
            log.error("==>短信发送失败：{}",e);
            return false;
        }
    }
}
