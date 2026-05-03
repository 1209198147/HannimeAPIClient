package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 下载信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadInfo {
    /**
     * 标题
     */
    private String title;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 浏览次数
     */
    private String views;
    /**
     * 封面图片
     */
    private String coverImg;
    /**
     * 下载项列表
     */
    private List<DownloadItem> downloadItems;
}
