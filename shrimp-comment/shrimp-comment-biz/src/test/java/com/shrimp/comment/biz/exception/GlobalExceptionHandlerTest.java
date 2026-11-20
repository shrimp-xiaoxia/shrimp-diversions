package com.shrimp.comment.biz.exception;

import com.shrimp.comment.biz.controller.CommentController;
import com.shrimp.comment.biz.model.vo.PublishCommentReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test void validationMessageDoesNotIncludeRejectedValue() throws Exception {
        var request = new MockHttpServletRequest("POST", "/comment/publish");
        var binding = new BeanPropertyBindingResult(new PublishCommentReqVO(), "comment");
        binding.addError(new FieldError("comment", "imageUrl", "sensitive-url-value", false,
                null, null, "评论图片地址不能超过 108 个字符"));
        var parameter = new MethodParameter(CommentController.class.getMethod("publishComment", PublishCommentReqVO.class), 0);
        var response = new GlobalExceptionHandler().handleMethodArgumentNotValidException(request,
                new MethodArgumentNotValidException(parameter, binding));
        assertFalse(response.isSuccess());
        assertEquals("COMMENT-10001", response.getErrorCode());
        assertTrue(response.getMessage().contains("imageUrl"));
        assertFalse(response.getMessage().contains("sensitive-url-value"));
    }

    @Test void validationErrorsUseStableFieldOrderWithoutValues() throws Exception {
        var request = new MockHttpServletRequest("POST", "/comment/publish");
        var binding = new BeanPropertyBindingResult(new PublishCommentReqVO(), "comment");
        binding.addError(new FieldError("comment", "noteId", "private-note-value", false, null, null, "笔记 ID 不能为空"));
        binding.addError(new FieldError("comment", "imageUrl", "private-image-value", false, null, null, "图片地址过长"));
        var parameter = new MethodParameter(CommentController.class.getMethod("publishComment", PublishCommentReqVO.class), 0);
        var response = new GlobalExceptionHandler().handleMethodArgumentNotValidException(request,
                new MethodArgumentNotValidException(parameter, binding));
        assertEquals("imageUrl 图片地址过长;noteId 笔记 ID 不能为空;", response.getMessage());
        assertFalse(response.getMessage().contains("private-"));
    }
}
