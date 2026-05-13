package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页 sections 数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePageSection {
    /**
     * 部分标题
     */
    private String title;

    /**
     * 查看更多链接
     */
    private String moreLink;

    /**
     * 视频信息列表
     */
    List<VideoInfo> videoInfoList;
}
