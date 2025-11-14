package com.shrimp.auth.alarm.Impl;

import com.shrimp.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmsAlarmHelper implements AlarmInterface {
    @Override
    public boolean send(String message) {
        log.info("==> 【短信告警】：{}", message);
        // 业务逻辑...
        return true;
    }
}
