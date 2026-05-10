package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.model.entities.*;
import com.shikou.model.entities.page.HomePage;
import com.shikou.model.entities.page.SearchPage;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
    public void testSearchPage() throws HanimeException {
        System.out.println("--- 获取搜索页面 ---");
        List<String> tags = List.of("中文字幕", "無碼", "同人作品");
        SearchParams searchParams = SearchParams.builder()
                .query("女仆")
                .tags(tags)
                .page(1)
                .build();
        SearchPage searchPage = client.getSearchPage(searchParams);
        List<String> genres = searchPage.getGenres();
        if (genres != null) {
            System.out.println("genres: " + genres.size());
            if (!genres.isEmpty()) {
                genres.forEach(genre -> {
                    System.out.println("\t" + genre);
                });
            }
        }
        Map<String, List<String>> tagsMap = searchPage.getTagsMap();
        if (tagsMap != null) {
            System.out.println("tags: " + tagsMap.size());
            if (!tagsMap.isEmpty()) {
                tagsMap.forEach((key, value) -> {
                    System.out.println("\t" + key);
                    if (!value.isEmpty()) {
                        System.out.println("\t\t" + value);
                    }
                });
            }
        }
        Map<String, String> dates = searchPage.getDates();
        if (dates != null) {
            System.out.println("dates: " + dates.size());
            if (!dates.isEmpty()) {
                dates.forEach((key, value) -> {
                    System.out.println("\t" + key);
                    if (!value.isEmpty()) {
                        System.out.println("\t\t" + value);
                    }
                });
            }
        }
        Map<String, String> durations = searchPage.getDurations();
        if (durations != null) {
            System.out.println("durations: " + durations.size());
            if (!durations.isEmpty()) {
                durations.forEach((key, value) -> {
                    System.out.println("\t" + key);
                    if (!value.isEmpty()) {
                        System.out.println("\t\t" + value);
                    }
                });
            }
        }
        List<VideoInfo> videos = searchPage.getVideos();
        if (videos != null) {
            System.out.println("videos: " + videos.size());
            if (!videos.isEmpty()) {
                for(int i=0; i < Math.min(10, videos.size()); i++){
                    VideoInfo videoInfo = videos.get(i);
                    System.out.println("\t" + videoInfo);
                }
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
            System.out.println("分辨率: " + downloadItem.getQuality());
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

    @Test
    public void testGetGenreList() throws HanimeNetworkException {
        List<String> genreList = client.getGenreList();
        System.out.println("类型大小: " + genreList.size());
        for (String genre : genreList) {
            System.out.println("类型：" + genre);
        }
    }

    @Test
    public void testGetTagsMap() throws HanimeNetworkException {
        Map<String, List<String>> tagsMap = client.getTagsMap();
        System.out.println("类型大小: " + tagsMap.size());
        for (Map.Entry<String, List<String>> tag : tagsMap.entrySet()) {
            System.out.println("类型：" + tag.getKey());
            for (String tagValue : tag.getValue()) {
                System.out.println("\t" + tagValue);
            }
        }
    }

    @Test
    public void testGetSortTypeList() throws HanimeNetworkException {
        List<String> sortTypeList = client.getSortTypeList();
        System.out.println("类型大小: " + sortTypeList.size());
        for (String sort : sortTypeList) {
            System.out.println("类型：" + sort);
        }
    }

    @Test
    public void testDownloadWithArgsVideoCode() throws Exception {
        System.out.println("--- 下载视频 ---");
        String videoCode = "404983";
        String quality = "480P";
        DownloadArgs args = DownloadArgs.builder()
                .videoCode(videoCode)
                .coverUrl("https://vdownload.hembed.com/image/thumbnail/404983h.jpg?secure=p_lfPZToChUe_STqyUHDMA==,1780194997")
                .videoName(videoCode)
                .quality(quality)
                .suffix("mp4")
                .build();
        client.download(args, (downloaded, total) -> {
            double percent = (double) downloaded / total * 100;
            System.out.printf("下载进度: %.1f%%%n", percent);
        });
        System.out.println("下载完成: " + args.getDownloadUrl() + "/" + args.getVideoName() + "." + args.getSuffix());
    }

    @Test
    public void testDownloadWithArgsUrl() throws Exception {
        System.out.println("--- 下载视频 ---");
        String videoCode = "404983";
        String quality = "480P";
        File outputFile = new File(System.getProperty("user.home") + "/Downloads", videoCode + ".mp4");
        DownloadArgs args = DownloadArgs.builder()
                .downloadUrl("https://vdownload.hembed.com/404983-sc-480p.mp4?secure=cLjJXaIXMENgGeS-Q6YAnw==,1778439608")
                .coverUrl("https://vdownload.hembed.com/image/thumbnail/404983h.jpg?secure=p_lfPZToChUe_STqyUHDMA==,1780194997")
                .quality(quality)
                .videoName(videoCode)
                .suffix("mp4")
                .build();
        client.download(args, (downloaded, total) -> {
            double percent = (double) downloaded / total * 100;
            System.out.printf("下载进度: %.1f%%%n", percent);
        });
        System.out.println("下载完成: " + outputFile.getAbsolutePath());
    }
}
