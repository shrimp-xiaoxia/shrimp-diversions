package com.shrimp.note.biz.constants;

/**
 * @author ccb
 */
public class RedisKeyConstants {
   private static final String NOTE_DETAIL_CACHE_KEY_PREFIX = "note:detail:cache:";

   public static String buildNoteDetailCacheKey(Long noteId) {
       return NOTE_DETAIL_CACHE_KEY_PREFIX + noteId;
   }
}
