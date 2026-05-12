package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 公共参数(页码、code、排序方式)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonParam {
    /** 页码，默认1 */
    @Builder.Default
    private int page = 1;

    /** code */
    @NotNull
    private String code;

    /** 排序方式 */
    private String sort;
}
