package com.shrimp.count.api;

import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.count.dto.CountSingleReqDTO;
import com.shrimp.framework.common.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "shrimp-count-biz")
public interface CountFeignApi {
    @PostMapping("/count/note/find")
    Response<CountResultDTO> findNote(@RequestBody CountSingleReqDTO request);
    @PostMapping("/count/user/find")
    Response<CountResultDTO> findUser(@RequestBody CountSingleReqDTO request);
    @PostMapping("/count/note/batch-find")
    Response<List<CountResultDTO>> findNotes(@RequestBody CountQueryReqDTO request);
    @PostMapping("/count/user/batch-find")
    Response<List<CountResultDTO>> findUsers(@RequestBody CountQueryReqDTO request);
}
