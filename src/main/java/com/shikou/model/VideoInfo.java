package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频信息（简要）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfo {

    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 影片观看URL，如 "https://hanimeone.me/watch?v=403047" */
    private String videoUrl;

    /** 封面/缩略图URL */
    private String coverUrl;

    /** 时长，如 "11:00" */
    private String duration;

    /** 好评率，如 "99%" */
    private String likeRate;

    /** 观看次数，如 "40.9萬次" */
    private String views;

    /** 标题 */
    private String title;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploader;

    /** 上传者搜索URL */
    private String uploaderUrl;

    /** 上传时间，如 "3個月前" */
    private String uploadTime;

    /** 类型 */
    private String genre;

    /** 是否正在播放 */
    @Builder.Default
    private boolean playing = false;

    /** 列表项类型: NORMAL=0, SIMPLIFIED=1 */
    @Builder.Default
    private int itemType = 0;
}
