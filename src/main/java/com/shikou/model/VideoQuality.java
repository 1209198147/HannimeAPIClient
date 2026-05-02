package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频画质信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoQuality {

    /** 分辨率标识，如 "1080P", "720P", "480P", "240P", "Unknown" */
    private String resolution;

    /** 视频直链URL */
    private String url;

    /** 文件后缀，如 "mp4", "webm" */
    private String suffix;
}
