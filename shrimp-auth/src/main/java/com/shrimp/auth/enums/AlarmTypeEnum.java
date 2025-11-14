package com.shrimp.auth.enums;

import com.shrimp.auth.alarm.AlarmInterface;
import com.shrimp.auth.alarm.Impl.MailAlarmHelper;
import com.shrimp.auth.alarm.Impl.SmsAlarmHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@Getter
@AllArgsConstructor
public enum AlarmTypeEnum {
    SMS("sms", SmsAlarmHelper.class),
    EMAIL("mail", MailAlarmHelper.class),
    ;

    private final String alarmType;
    private final Class<? extends AlarmInterface> clazz;
}
