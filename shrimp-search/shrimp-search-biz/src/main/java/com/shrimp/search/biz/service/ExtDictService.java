package com.shrimp.search.biz.service;

import org.springframework.http.ResponseEntity;

/**
 * @author ccb
 */
public interface ExtDictService{
    ResponseEntity<String> getHotUpdateExtDict();
}
