package com.shrimp.auth.alarm;

import com.shrimp.auth.enums.AlarmTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RefreshScope
public class AlarmConfig {
    @Value(value = "${alarm.type}")
    private String alarmType;

    @Bean
    @RefreshScope
    public AlarmInterface alarmHelper() {
//        Arrays.stream(AlarmTypeEnum.values())
//                .forEach(enumType -> {
//                    if(enumType.getAlarmType().equals(alarmType)) {
//                        try {
//                            AlarmInterface alarmHelper = enumType.getClazz().getDeclaredConstructor().newInstance();
//
//                        } catch (Exception e) {
//                            throw new IllegalArgumentException("错误的告警类型...");
//                        }
//                    }
//                });
//        throw new IllegalArgumentException("错误的告警类型...");
        List<AlarmTypeEnum> list = Arrays.stream(AlarmTypeEnum.values()).toList();
        for (AlarmTypeEnum alarmTypeEnum : list) {
            if (alarmTypeEnum.getAlarmType().equals(alarmType)) {
                try {
                    return alarmTypeEnum.getClazz().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("错误的告警类型...");
                }
            }
        }
        throw new IllegalArgumentException("错误的告警类型...");
    }
}
