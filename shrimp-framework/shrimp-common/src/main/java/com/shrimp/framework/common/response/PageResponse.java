package com.shrimp.framework.common.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> extends Response<List<T>> {

    private long pageNo;
    private long totalCount;
    private long pageSize;
    private long totalPage;

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount) {
        PageResponse<T> pageResponse = new PageResponse<>();

        pageResponse.setSuccess(true);
        pageResponse.setData(data);

        pageResponse.setPageNo(pageNo);
        long pageSize = 10L;
        pageResponse.setPageSize(pageSize);
        pageResponse.setTotalCount(totalCount);
        long totalPage = getTotalPage(totalCount, pageSize);
        pageResponse.setTotalPage(totalPage);

        return pageResponse;
    }

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount,long pageSize) {
        PageResponse<T> pageResponse = new PageResponse<>();

        pageResponse.setSuccess(true);
        pageResponse.setData(data);

        pageResponse.setPageNo(pageNo);
        pageResponse.setPageSize(pageSize);
        pageResponse.setTotalCount(totalCount);
        long totalPage = getTotalPage(totalCount, pageSize);
        pageResponse.setTotalPage(totalPage);

        return pageResponse;
    }



    public static long getTotalPage(long totalCount, long pageSize) {
        return totalCount == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

    public static long getOffset(long pageNo,long pageSize){
        if(pageNo < 1){
            pageNo = 1;
        }
        return (pageNo - 1) * pageSize;
    }
}
