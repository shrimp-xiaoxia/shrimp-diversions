package com.shrimp.distrituted.id.generator.api;

import com.shrimp.distrituted.id.generator.constants.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author ccb
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface DistributedIdGeneratorFeignApi {
    String PREFIX = "/id";

    @GetMapping(PREFIX + "/segment/get/{key}")
    String getSegmentId(@PathVariable("key") String key);

    @GetMapping(PREFIX + "/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);
}
