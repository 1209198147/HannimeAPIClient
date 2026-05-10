package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 轮播Banner信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 影片观看URL，如 "https://hanimeone.me/watch?v=403047" */
    private String videoUrl;

    /** 图片URL */
    private String picUrl;

    /** 观看次数，如 "40.9萬次" */
    private String views;

    /** 标题 */
    private String title;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploader;

    /** 上传时间，如 "3個月前" */
    private String uploadTime;

    /** 标签列表 */
    private List<String> tags;
}
