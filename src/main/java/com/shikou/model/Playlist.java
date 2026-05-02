package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 播放列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {

    /** 播放列表代码 */
    private String listCode;

    /** 标题 */
    private String title;

    /** 影片总数 */
    @Builder.Default
    private int total = 0;

    /** 影片列表 */
    private List<VideoInfo> videos;
}
