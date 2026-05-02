package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewInfo {

    /** 标题 */
    private String title;

    /** 视频标题 */
    private String videoTitle;

    /** 封面URL */
    private String coverUrl;

    /** 简介 */
    private String introduction;

    /** 品牌 */
    private String brand;

    /** 发布日期 */
    private String releaseDate;

    /** 影片代码 */
    private String videoCode;

    /** 标签列表 */
    private List<String> tags;

    /** 相关图片URL */
    private String relatedPicsUrl;
}
