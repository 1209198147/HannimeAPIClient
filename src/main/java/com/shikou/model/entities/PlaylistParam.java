package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 播放列表相关的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistParam {
    @NotNull
    private String listCode;
    @Builder.Default
    private int page = 1;
    /** 排序方式 */
    private String sort;
}
