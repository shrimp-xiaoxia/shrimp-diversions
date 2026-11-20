package com.shrimp.search.biz.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.shrimp.framework.common.constants.DateConstants;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.framework.common.util.DateUtils;
import com.shrimp.framework.common.util.NumberUtils;
import com.shrimp.search.biz.domain.mapper.SelectMapper;
import com.shrimp.search.biz.enums.NotePublishTimeRangeEnum;
import com.shrimp.search.biz.enums.NoteSortTypeEnum;
import com.shrimp.search.biz.enums.ResponseCodeEnum;
import com.shrimp.search.biz.index.NoteIndex;
import com.shrimp.search.biz.index.NoteCardFields;
import com.shrimp.search.biz.model.vo.SearchNoteReqVO;
import com.shrimp.search.biz.model.vo.SearchNoteRspVO;
import com.shrimp.search.biz.service.NoteService;
import com.shrimp.search.dto.RebuildNoteDocumentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ccb
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    @Override
    public PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO) {
        //提取关键字和页码
        String keyword = searchNoteReqVO.getKeyword();
        Integer pageNo = searchNoteReqVO.getPageNo();
        Integer type = searchNoteReqVO.getType();
        Integer sort = searchNoteReqVO.getSort();
        Integer publishTimeRange = searchNoteReqVO.getPublishTimeRange();
        // 构建 SearchRequest，指定要查询的索引
        SearchRequest searchRequest = new SearchRequest(NoteIndex.NAME);
        // 创建查询构建器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.multiMatchQuery(keyword)
                .field(NoteIndex.FIELD_NOTE_TITLE, 2.0f)
                .field(NoteIndex.FIELD_NOTE_TOPIC));

//        设置过滤条件
        if (ObjUtil.isNotNull(type)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(NoteIndex.FIELD_NOTE_TYPE, type));
        }
        if (ObjUtil.isNotNull(publishTimeRange)) {
            NotePublishTimeRangeEnum notePublishTimeRangeEnum = NotePublishTimeRangeEnum.valueOf(publishTimeRange);
            if (ObjUtil.isNull(notePublishTimeRangeEnum)) {
                throw new BizException(ResponseCodeEnum.PUBLISH_TIME_TYPE_FAIL);
            }
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = null;
            switch (notePublishTimeRangeEnum) {
                case DAY -> start = end.minusDays(1L);
                case WEEK -> start = end.minusWeeks(1L);
                case HALF_YEAR -> start = end.minusMonths(6L);
            }
            boolQueryBuilder.filter(QueryBuilders.rangeQuery(NoteIndex.FIELD_NOTE_CREATE_TIME)
                    .gte(DateUtils.localDateTime2String(start))
                    .lte(DateUtils.localDateTime2String(end)));
        }

//        定义其搜索的排序方式
        searchDefineSort(boolQueryBuilder, searchSourceBuilder, sort);

        int pageSize = 10;
        int fromIndex = (pageNo - 1) * pageSize;
        searchSourceBuilder.from(fromIndex);
        searchSourceBuilder.size(pageSize);

        // 设置高亮字段
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(NoteIndex.FIELD_NOTE_TITLE)
                .preTags("<strong>")
                .postTags("</strong>");
        searchSourceBuilder.highlighter(highlightBuilder);
        // 将构建的查询条件设置到 SearchRequest 中
        searchRequest.source(searchSourceBuilder);

        List<SearchNoteRspVO> searchNoteRspVOList = null;
        long total = 0L;
        try {
            log.info("==> SearchRequest: {}", searchRequest.source().toString());
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);

            SearchHits hits = searchResponse.getHits();
            searchNoteRspVOList = new ArrayList<>(pageSize);
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                long noteId = ((Number) sourceAsMap.get(NoteIndex.FIELD_NOTE_ID)).longValue();
                NoteCardFields cardFields = NoteCardFields.from(sourceAsMap);
                String cover = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_COVER);
                String title = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_TITLE);
                int likeCount = 0;
                Object likeCountObj = sourceAsMap.get(NoteIndex.FIELD_NOTE_LIKE_TOTAL);
                if (ObjUtil.isNotNull(likeCountObj)) {
                    likeCount = (Integer) likeCountObj;
                }
                int collectCount = 0;
                Object collectCountObj = sourceAsMap.get(NoteIndex.FIELD_NOTE_COLLECT_TOTAL);
                if (ObjUtil.isNotNull(collectCountObj)) {
                    collectCount = (Integer) collectCountObj;
                }
                int commentCount = 0;
                Object commentCountObj = sourceAsMap.get(NoteIndex.FIELD_NOTE_COMMENT_TOTAL);
                if (ObjUtil.isNotNull(commentCountObj)) {
                    commentCount = (Integer) commentCountObj;
                }

                String updateTimeStr = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_UPDATE_TIME);
                LocalDateTime updateTime = LocalDateTime.parse(updateTimeStr, DateConstants.DATE_FORMAT_Y_M_D_H_M_S);
                String highlightTitle = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields()) &&
                        hit.getHighlightFields().containsKey(NoteIndex.FIELD_NOTE_TITLE)) {
                    highlightTitle = hit.getHighlightFields().get(NoteIndex.FIELD_NOTE_TITLE).fragments()[0].toString();
                }

                SearchNoteRspVO searchNoteRspVO = SearchNoteRspVO.builder()
                        .noteId(noteId)
                        .nickname(cardFields.nickname())
                        .avatar(cardFields.avatar())
                        .creatorId(cardFields.creatorId())
                        .type(cardFields.type())
                        .videoUri(cardFields.videoUri())
                        .cover(cover)
                        .title(title)
                        .likeCount(NumberUtils.formatNumberString(likeCount))
                        .collectTotal(NumberUtils.formatNumberString(collectCount))
                        .commentTotal(NumberUtils.formatNumberString(commentCount))
                        .highlightTitle(highlightTitle)
                        .updateTime(DateUtils.formatRelativeTime(updateTime))
                        .build();

                searchNoteRspVOList.add(searchNoteRspVO);
            }
        } catch (Exception e) {
            log.error("==>  查询 Elasticserach 异常", e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        return PageResponse.success(searchNoteRspVOList, pageNo, total);
    }
    /**
     * 重建笔记文档
     *
     * @param rebuildNoteDocumentReqDTO
     * @return
     */
    @Override
    public Response<Long> rebuildDocument(RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO) {
        Long noteId = rebuildNoteDocumentReqDTO.getId();

        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectEsNoteIndexData(noteId, null);

        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("==> 重建笔记文档失败: ", e);
            }
        }
        return Response.success();
    }

    private static void searchDefineSort(BoolQueryBuilder boolQueryBuilder, SearchSourceBuilder searchSourceBuilder, Integer sort) {
        if (ObjUtil.isNull(sort)) {
            // 按照 _score 降序
            searchSourceBuilder.sort(new FieldSortBuilder("_score").order(SortOrder.DESC));
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                                    .factor(0.5f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    ),
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                                    .factor(0.3f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    ),
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                                    .factor(0.2f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    )
            };

            FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolQueryBuilder, filterFunctionBuilders)
                    .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                    .boostMode(CombineFunction.SUM);
            searchSourceBuilder.query(functionScoreQueryBuilder);
            return;
        }

        NoteSortTypeEnum noteSortTypeEnum = NoteSortTypeEnum.valueOf(sort);
        if (ObjUtil.isNull(noteSortTypeEnum)) {
            throw new BizException(ResponseCodeEnum.SORT_TYPE_FAIL);
        }

        switch (noteSortTypeEnum) {
            case LATEST ->
                    searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_CREATE_TIME).order(SortOrder.DESC));
            case MOST_LIKE ->
                    searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL).order(SortOrder.DESC));
            case MOST_COLLECT ->
                    searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL).order(SortOrder.DESC));
            case MOST_COMMENT ->
                    searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL).order(SortOrder.DESC));
        }
        // 设置查询
        searchSourceBuilder.query(boolQueryBuilder);
    }

}
