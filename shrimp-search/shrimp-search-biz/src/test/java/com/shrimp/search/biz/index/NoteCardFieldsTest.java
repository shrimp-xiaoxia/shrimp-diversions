package com.shrimp.search.biz.index;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NoteCardFieldsTest {
    @Test void mapsCurrentIndexFieldsWithoutLosingLargeIds() {
        var fields = NoteCardFields.from(Map.of("creator_nickname", "作者", "creator_avatar", "avatar",
                "creator_id", 1895795420446070646L, "type", 1, "video_uri", "video"));
        assertEquals("作者", fields.nickname());
        assertEquals("avatar", fields.avatar());
        assertEquals(1895795420446070646L, fields.creatorId());
        assertEquals(1, fields.type());
        assertEquals("video", fields.videoUri());
    }

    @Test void supportsOlderDocumentsWithoutInventingFields() {
        var fields = NoteCardFields.from(Map.of("nickname", "旧作者", "type", 0));
        assertEquals("旧作者", fields.nickname());
        assertNull(fields.creatorId());
        assertNull(fields.videoUri());
        assertEquals(0, fields.type());
    }
}
