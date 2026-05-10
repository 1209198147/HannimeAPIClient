package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItem {

    /** 列表代码，如 "WL"(稍后观看), "LL"(喜欢的影片), "SL"(订阅) */
    private String listCode;

    /** 列表标题 */
    private String title;

    /** 影片总数 */
    @Builder.Default
    private int total = 0;
}
