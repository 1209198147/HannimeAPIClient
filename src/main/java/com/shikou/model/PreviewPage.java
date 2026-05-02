package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览页面
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewPage {

    /** 头部图片URL */
    private String headerPicUrl;

    /** 是否有上一页 */
    @Builder.Default
    private boolean hasPrevious = false;

    /** 是否有下一页 */
    @Builder.Default
    private boolean hasNext = false;

    /** 最新影片列表 */
    private List<VideoInfo> latestHanime;

    /** 预览信息列表 */
    private List<PreviewInfo> previewInfoList;
}
