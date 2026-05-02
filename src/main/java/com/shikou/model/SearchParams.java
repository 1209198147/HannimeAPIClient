package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** 类型 */
    private String genre;

    /** 排序方式 */
    private String sort;

    /** 模糊搜索，"on"启用 */
    private String broad;

    /** 发布年份 */
    private Integer year;

    /** 发布月份 */
    private Integer month;

    /** 时长 */
    private String duration;

    /** 标签集合 */
    private Set<String> tags;

    /** 品牌集合 */
    private Set<String> brands;

    public SearchParams(String query) {
        this.query = query;
    }
}
