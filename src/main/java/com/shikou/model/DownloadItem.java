package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可下载项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadItem {
    /** 分辨率标识，如 "1080P", "720P", "480P", "240P", "Unknown" */
    private String resolution;
    /** 列表项类型: mp4 */
    private String itemType;
    /** 文件大小 - 此属性无用*/
    @Deprecated
    private String size;
    /** 下载链接 */
    private String downloadUrl;
}
