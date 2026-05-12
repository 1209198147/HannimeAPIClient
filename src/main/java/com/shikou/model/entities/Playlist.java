package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 播放列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playlist extends Pagination {
    /** 播放列表代码 */
    private String listCode;

    /** 播放列表链接 */
    private String listUrl;

    /** 标题 */
    private String title;

    /** 封面图片链接 (第一个视频的封面) */
    private String coverUrl;

    /** 观看次数 */
    private String views;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploader;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploaderAvatarUrl;

    /** 上传者搜索URL */
    private String uploaderUrl;

    /** 描述 */
    private String description;

    /**
     * 排序方式
     */
    private Map<String, String> sort;

    /** 影片总数 */
    @Builder.Default
    private int total = 0;

    /** 影片列表 */
    private List<VideoInfo> videos;
}
