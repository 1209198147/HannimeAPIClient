package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagination {
    /**
     * 总页数
     */
    private int totalPage = 1;
    /**
     * 当前页
     */
    private int currentPage = 1;
    /**
     * 是否有上一页
     */
    private boolean hasPrevPage;
    /**
     * 是否有下一页
     */
    private boolean hasNextPage;
}
