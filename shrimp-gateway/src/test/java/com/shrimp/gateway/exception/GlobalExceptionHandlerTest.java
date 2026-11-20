package com.shrimp.gateway.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test
    void invalidTokenIsNotIncludedInResponseOrLog() throws Exception {
        var handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", new ObjectMapper());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/protected").build());
        var logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        String sensitiveValue = "synthetic-invalid-token";
        try {
            handler.handle(exchange, new NotLoginException("token 无效: " + sensitiveValue, null, null)).block();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            var body = new ObjectMapper().readTree(exchange.getResponse().getBodyAsString().block());
            assertEquals("401", body.get("errorCode").asText());
            assertFalse(body.toString().contains(sensitiveValue));
            assertTrue(appender.list.stream().noneMatch(event ->
                    event.getFormattedMessage().contains(sensitiveValue) || event.getThrowableProxy() != null));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
