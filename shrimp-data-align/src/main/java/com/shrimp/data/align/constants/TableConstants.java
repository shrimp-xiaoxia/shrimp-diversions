package com.shrimp.data.align.constants;


/**
 * @author ccb
 */
public class TableConstants {

    private static final String TABLE_NAME_SEPARATE = "_";

    public static String buildTableNameSuffix(String date, long hasKey) {
        return date + TABLE_NAME_SEPARATE + hasKey;
    }
}
