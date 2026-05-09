package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeException;
import com.shikou.model.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class HanimeapiClientTest {
    private HanimeApiClient client;
    private static final String MODE = "HK";

    @Before
    public void setUp() {
        // 也可以使用自定义配置
        if ("HK".equalsIgnoreCase(MODE)) {
            client = HanimeApiClient.builder()
                    .config(HanimeConfig.defaultConfigHK())
                    .build();
        }else{
            client = HanimeApiClient.builder()
                    .config(HanimeConfig.defaultConfig())
                    .build();
        }
    }


    @Test
    public void testHomePage() throws HanimeException {
        System.out.println("--- 获取首页 ---");
        HomePage homePage = client.getHomePage();
        if (homePage.getSections() != null) {
            System.out.println("内容部分数: " + homePage.getSections().size());
            if (!homePage.getSections().isEmpty()) {
                homePage.getSections().forEach(homePageSection -> {
                    System.out.println("  " + homePageSection.getTitle() + ": " + homePageSection.getVideoInfoList().size() + " 部");
                });
            }
        }
        System.out.println();
    }

    @Test
    public void testSearch() throws HanimeException {
        System.out.println("--- 搜索影片 ---");
        SearchParams searchParams = SearchParams.builder()
                .query("女仆")
                .page(1)
                .build();
        List<VideoInfo> searchResults = client.search(searchParams);
        System.out.println("搜索结果数量: " + searchResults.size());
        for (int i = 0; i < Math.min(3, searchResults.size()); i++) {
            VideoInfo info = searchResults.get(i);
            System.out.println("  [" + (i + 1) + "] " + info.getTitle() + " | 代码: " + info.getVideoCode());
        }
        System.out.println();
    }

    @Test
    public void testVideoDetail() throws HanimeException {
        System.out.println("--- 获取影片详情 ---");
        String videoCode = "405939";
        HanimeVideo video = client.getVideoDetail(videoCode);
        System.out.println("标题: " + video.getTitle());
        System.out.println("中文标题: " + video.getChineseTitle());
        System.out.println("作者: " + video.getArtist().getName());
        System.out.println("简介: " + video.getIntroduction());
        System.out.println("系列播放列表: " + video.getPlaylist().getTotal() + " 集");
        System.out.println("封面: " + video.getCoverUrl());
        System.out.println("观看次数: " + video.getViews());
        System.out.println("上传时间: " + video.getUploadTime());
        System.out.println("标签: " + video.getTags());
        if (video.getVideoUrls() != null && !video.getVideoUrls().isEmpty()) {
            System.out.println("可用画质: " + video.getVideoUrls().keySet());
        }
        System.out.println();
    }

    @Test
    public void testDownloadInfo() throws HanimeException {
        DownloadInfo downloadInfo = client.getDownloadInfo("405939");

        System.out.println("标题: " + downloadInfo.getTitle());
        System.out.println("观看次数: " + downloadInfo.getViews());
        System.out.println("封面: " + downloadInfo.getCoverImg());
        System.out.println("上传时间: " + downloadInfo.getUpdateTime());
        List<DownloadItem> downloadItems = downloadInfo.getDownloadItems();
        for (DownloadItem downloadItem : downloadItems) {
            System.out.println("--------------------");
            System.out.println("分辨率: " + downloadItem.getResolution());
            System.out.println("下载链接: " + downloadItem.getDownloadUrl());
            System.out.println("类型: " + downloadItem.getItemType());
        }
    }

    @Test
    public void testDownload() throws Exception {
        System.out.println("--- 下载视频 ---");
        String videoCode = "404983";
        File outputFile = new File(System.getProperty("user.home") + "/Downloads", videoCode + ".mp4");
        client.download(videoCode, "720P", outputFile, (downloaded, total) -> {
            double percent = (double) downloaded / total * 100;
            System.out.printf("下载进度: %.1f%%%n", percent);
        });
        System.out.println("下载完成: " + outputFile.getAbsolutePath());
    }
}
