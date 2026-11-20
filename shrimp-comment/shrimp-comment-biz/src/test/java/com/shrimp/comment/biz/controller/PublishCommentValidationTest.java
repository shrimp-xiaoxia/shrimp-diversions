package com.shrimp.comment.biz.controller;

import com.shrimp.comment.biz.model.vo.PublishCommentReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublishCommentValidationTest {
    @Test void rejectsImageUrlsLongerThanDatabaseColumn() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = PublishCommentReqVO.builder().noteId(1L).imageUrl("a".repeat(108)).build();
            assertEquals(0, validator.validate(request).size());
            request.setImageUrl("a".repeat(109));
            assertEquals(1, validator.validate(request).size());
            request.setImageUrl(null);
            assertEquals(0, validator.validate(request).size());
        }
    }
}
