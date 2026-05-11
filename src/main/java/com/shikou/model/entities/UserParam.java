package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 用户页面相关的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserParam {
    /** 页码，默认1 */
    @Builder.Default
    private int page = 1;

    /** 用户ID */
    @NotNull
    private String userId;

    /** 排序方式 */
    private String sort;
}
