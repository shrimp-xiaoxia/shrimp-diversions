package com.shrimp.search.biz.index;

import java.util.Map;

/** 兼容历史索引和当前同步链路的字段名。 */
public record NoteCardFields(String nickname, String avatar, Long creatorId, Integer type, String videoUri) {
    public static NoteCardFields from(Map<String, Object> source) {
        return new NoteCardFields(
                text(source, "creator_nickname", "nickname"),
                text(source, "creator_avatar", "avatar"),
                longValue(source.get("creator_id")),
                intValue(source.get("type")),
                text(source, "video_uri", "videoUri"));
    }

    private static String text(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        if (value == null) value = source.get(fallback);
        return value instanceof String text ? text : null;
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try { return Long.valueOf(text); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static Integer intValue(Object value) {
        Long result = longValue(value);
        return result != null && result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE ? result.intValue() : null;
    }
}
