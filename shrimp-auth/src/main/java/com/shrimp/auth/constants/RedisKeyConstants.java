package com.shrimp.auth.constants;

/**
 * @author ccb
 */
public class RedisKeyConstants {
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

}
