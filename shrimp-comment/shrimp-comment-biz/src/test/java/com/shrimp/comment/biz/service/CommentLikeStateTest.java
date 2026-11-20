package com.shrimp.comment.biz.service;
import com.shrimp.comment.biz.service.Impl.CommentServiceImpl;
import com.shrimp.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.shrimp.comment.biz.model.vo.LikeCommentReqVO;
import com.shrimp.comment.biz.model.vo.UnlikeCommentReqVO;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class CommentLikeStateTest {
 @Test void duplicateLikeIsRejectedAndUnlikeAllowsSubsequentLike() {
  var service=new CommentServiceImpl();
  RedisTemplate<String,Object> redis=mock(RedisTemplate.class);
  ValueOperations<String,Object> values=mock(ValueOperations.class);
  var mapper=mock(CommentLikeDOMapper.class); var mq=mock(RocketMQTemplate.class);
  when(redis.opsForValue()).thenReturn(values);
  when(values.get(contains("comment:like:state:"))).thenReturn(null,true,true,false);
  when(values.get(argThat(k -> k != null && !k.toString().contains("comment:like:state:")))).thenReturn("exists");
  when(values.setIfAbsent(anyString(),any(),eq(30L),eq(TimeUnit.SECONDS))).thenReturn(true);
  when(mapper.selectCountByCommentIdAndUserId(123L,456L)).thenReturn(0L);
  ReflectionTestUtils.setField(service,"redisTemplate",redis);
  ReflectionTestUtils.setField(service,"commentLikeDOMapper",mapper);
  ReflectionTestUtils.setField(service,"rocketMQTemplate",mq);
  LoginUserContextHolder.setLoginUserId(456L);
  try {
   assertTrue(service.likeComment(new LikeCommentReqVO(123L)).isSuccess());
   assertThrows(BizException.class,()->service.likeComment(new LikeCommentReqVO(123L)));
   assertTrue(service.unlikeComment(new UnlikeCommentReqVO(123L)).isSuccess());
   assertTrue(service.likeComment(new LikeCommentReqVO(123L)).isSuccess());
   verify(mq,times(3)).syncSendOrderly(anyString(),any(Message.class),eq("456"));
  } finally { LoginUserContextHolder.removeLoginUserContext(); }
 }
}
