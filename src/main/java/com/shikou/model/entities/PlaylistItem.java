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

    /** 列表代码 */
    private String listCode;

    /** 列表链接 */
    private String listUrl;

    /** 封面图片链接 */
    private String coverUrl;

    /** 列表标题 */
    private String title;

    /** 上传者 */
    private String uploader;

    /** 上传者链接 */
    private String uploaderUrl;

    /** 上传时间 */
    private String uploadTime;

    /** 影片总数 */
    @Builder.Default
    private int total = 0;
}
