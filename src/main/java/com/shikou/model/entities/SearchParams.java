package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 搜索参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchParams {

    /** 页码，默认1 */
    @Builder.Default
    private int page = 1;

    /** 搜索关键词 */
    private String query;

    /** 类型 - 无用 */
    @Deprecated
    private String type;

    /** 类型 */
    private String genre;

    /** 排序方式 */
    private String sort;

    /** 日期 */
    private String date;

    /** 时长 */
    private String duration;

    /** 标签 */
    private List<String> tags;

    public SearchParams(String query) {
        this.query = query;
    }
}
