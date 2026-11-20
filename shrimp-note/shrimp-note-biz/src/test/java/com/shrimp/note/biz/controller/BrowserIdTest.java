package com.shrimp.note.biz.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrimp.framework.common.annotation.BrowserId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class BrowserIdTest {
    static class Payload {
        @BrowserId public Long id = 9007199254740993L;
        public Long likeTotal = 12L;
    }
    @Test void onlyIdsAreSerializedAsStrings() throws Exception {
        assertEquals("{\"id\":\"9007199254740993\",\"likeTotal\":12}", new ObjectMapper().writeValueAsString(new Payload()));
    }
}
