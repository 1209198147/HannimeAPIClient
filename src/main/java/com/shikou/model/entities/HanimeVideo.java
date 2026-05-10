package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 影片详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HanimeVideo {

    /** 英文标题 */
    private String title;

    /** 中文标题 */
    private String chineseTitle;

    /** 封面URL */
    private String coverUrl;

    /** 简介 */
    private String introduction;

    /** 上传时间 */
    private String uploadTime;

    /** 观看次数 */
    private String views;

    /** 多分辨率视频URL映射: 分辨率 -> VideoQuality */
    private Map<String, VideoQuality> videoUrls;

    /** 标签列表 */
    private List<String> tags;

    /** 用户播放列表信息 */
    private MyList myList;

    /** 系列播放列表 */
    private Playlist playlist;

    /** 相关影片 */
    private List<VideoInfo> relatedHanimes;

    /** 作者信息 */
    private Artist artist;

    /** 收藏数 */
    private Integer favTimes;

    /** 是否已收藏 */
    @Builder.Default
    private boolean fav = false;
}
