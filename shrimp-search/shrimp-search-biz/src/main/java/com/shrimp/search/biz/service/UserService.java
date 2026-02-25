package com.shrimp.search.biz.service;

import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.search.biz.model.vo.SearchUserReqVO;
import com.shrimp.search.biz.model.vo.SearchUserRspVO;
import com.shrimp.search.dto.RebuildUserDocumentReqDTO;

/**
 * @author ccb
 */
public interface UserService {
    PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO);

    /**
     * 重建用户文档
     * @param rebuildUserDocumentReqDTO
     * @return
     */
    Response<Long> rebuildDocument(RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO);
}
