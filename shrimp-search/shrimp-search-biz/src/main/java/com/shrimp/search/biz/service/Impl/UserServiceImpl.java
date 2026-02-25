package com.shrimp.search.biz.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.NumberUtils;

import com.shrimp.search.biz.domain.mapper.SelectMapper;
import com.shrimp.search.biz.index.UserIndex;
import com.shrimp.search.biz.model.vo.SearchUserReqVO;
import com.shrimp.search.biz.model.vo.SearchUserRspVO;
import com.shrimp.search.biz.service.UserService;
import com.shrimp.search.dto.RebuildUserDocumentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ccb
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
//        提取关键字和页码
        String keyword = searchUserReqVO.getKeyword();
        Integer pageNo = searchUserReqVO.getPageNo();
//        构建searchAPI
        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_XIAOHASHU_ID));
        FieldSortBuilder sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FANS_TOTAL).order(SortOrder.DESC);
        searchSourceBuilder.sort(sortBuilder);
        int pageSize = 10;
        int fromIndex = (pageNo - 1) * pageSize;
        searchSourceBuilder.from(fromIndex);
        searchSourceBuilder.size(pageSize);
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(UserIndex.FIELD_USER_NICKNAME)
                .preTags("<strong>")
                .postTags("</strong>");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);

        List<SearchUserRspVO> searchUserRspVOList = new ArrayList<>();
        long total = 0L;

        try {
            log.info("==> SearchRequest: {}", searchRequest);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            long hitTotal = searchResponse.getHits().getTotalHits().value;
            log.info("==> 搜索到结果数:hits {}", hitTotal);

            SearchHits hits = searchResponse.getHits();

            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                long userId = ((Number) sourceAsMap.get(UserIndex.FIELD_USER_ID)).longValue();
                String nickname = (String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME);
                String userXiaoHaShuId = (String) sourceAsMap.get(UserIndex.FIELD_USER_XIAOHASHU_ID);
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);

                int noteTotal = 0;
                Object noteTotalObj = sourceAsMap.get(UserIndex.FIELD_USER_NOTE_TOTAL);
                if (ObjUtil.isNotNull(noteTotalObj)) {
                    noteTotal = (Integer) noteTotalObj;
                }

                int fansTotal = 0;
                Object fansTotalObj = sourceAsMap.get(UserIndex.FIELD_USER_FANS_TOTAL);
                if (ObjUtil.isNotNull(fansTotalObj)) {
                    fansTotal = (Integer) fansTotalObj;
                }

                // 获取高亮字段
                String highlightedNickname = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields()) && hit.getHighlightFields().containsKey(UserIndex.FIELD_USER_NICKNAME)) {
                    highlightedNickname = hit.getHighlightFields().get(UserIndex.FIELD_USER_NICKNAME).fragments()[0].string();
                }

                SearchUserRspVO searchUserRspVO = SearchUserRspVO.builder()
                        .userId(userId)
                        .nickname(nickname)
                        .xiaohashuId(userXiaoHaShuId)
                        .avatar(avatar)
                        .noteTotal(noteTotal)
                        .fansTotal(NumberUtils.formatNumberString(fansTotal))
                        .highlightNickname(highlightedNickname)
                        .build();
                searchUserRspVOList.add(searchUserRspVO);
            }

        } catch (
                Exception e) {
            log.error("==> 查询 Elasticserach 异常: ", e);
        }
        return PageResponse.success(searchUserRspVOList, pageSize, total);
    }

    /**
     * 重建用户文档
     *
     * @param rebuildUserDocumentReqDTO
     * @return
     */
    @Override
    public Response<Long> rebuildDocument(RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO) {
        Long userId = rebuildUserDocumentReqDTO.getId();

        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectEsUserIndexData(userId);

        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("==> 重建用户文档异常: ", e);
            }
        }
        return Response.success();
    }

}
