package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadArgs {

    /** 画质，如 "1080P" */
    private String quality;

    /** 视频直链URL */
    private String downloadUrl;

    /** 文件后缀，如 "mp4" */
    private String suffix;

    /** 影片名称（用于文件名） */
    private String videoName;

    /** 影片代码 */
    private String videoCode;

    /** 封面URL */
    private String coverUrl;
}
