package com.itheima.smartcodenote.common;

import java.util.List;
import lombok.Getter;

@Getter
public class PageResponse<T> {

    private final List<T> records;
    private final long total;
    private final long pageNum;
    private final long pageSize;

    private PageResponse(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResponse<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResponse<>(records, total, pageNum, pageSize);
    }
}
