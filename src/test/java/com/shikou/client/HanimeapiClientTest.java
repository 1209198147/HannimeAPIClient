package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.model.entities.*;
import com.shikou.model.entities.page.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
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
    public void testHomePage() throws Exception {
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
    public void testSearchPage() throws Exception {
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
        System.out.println("总页数: " + searchPage.getTotalPage());
        System.out.println("是否有上一页: " + searchPage.isHasPrevPage());
        System.out.println("是否有下一页: " + searchPage.isHasNextPage());
        System.out.println();
    }

    @Test
    public void testSearch() throws Exception {
        System.out.println("--- 搜索影片 ---");
        SearchParams searchParams = SearchParams.builder()
                .query("女仆")
                .page(1)
                .build();
        SearchResult searchResult = client.search(searchParams);
        List<VideoInfo> videos = searchResult.getVideos();
        System.out.println("搜索结果数量: " + searchResult.getVideos().size());
        System.out.println("总页数: " + searchResult.getTotalPage());
        System.out.println("是否有上一页: " + searchResult.isHasPrevPage());
        System.out.println("是否有下一页: " + searchResult.isHasNextPage());
        for (int i = 0; i < Math.min(3, videos.size()); i++) {
            VideoInfo info = videos.get(i);
            System.out.println("  [" + (i + 1) + "] " + info.getTitle() + " | 代码: " + info.getVideoCode());
        }
        System.out.println();
    }

    @Test
    public void testVideoDetail() throws Exception {
        System.out.println("--- 获取影片详情 ---");
        String videoCode = "405939";
        HanimeVideo video = client.getVideoDetail(videoCode);
        System.out.println("标题: " + video.getTitle());
        System.out.println("中文标题: " + video.getChineseTitle());
        System.out.println("作者: " + video.getArtist().getName());
        System.out.println("简介: " + video.getIntroduction());
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
    public void testGetWatchPage() throws Exception {
        System.out.println("--- 获取观看页面 ---");
        String videoCode = "405939";
        WatchPage watchPage = client.getWatchPage(videoCode);
        System.out.println("标题: " + watchPage.getTitle());
        System.out.println("中文标题: " + watchPage.getChineseTitle());
        System.out.println("作者: " + watchPage.getArtist().getName());
        System.out.println("简介: " + watchPage.getIntroduction());
        System.out.println("系列播放列表: " + watchPage.getPlaylist().getTotal() + " 集");
        System.out.println("封面: " + watchPage.getCoverUrl());
        System.out.println("观看次数: " + watchPage.getViews());
        System.out.println("上传时间: " + watchPage.getUploadTime());
        System.out.println("标签: " + watchPage.getTags());
        if (watchPage.getVideoUrls() != null && !watchPage.getVideoUrls().isEmpty()) {
            System.out.println("可用画质: " + watchPage.getVideoUrls().keySet());
        }
        System.out.println();
    }

    @Test
    public void testGetProfile() throws Exception {
        System.out.println("--- 获取用户Profile ---");
        String userId = "995886";
        Profile profile = client.getProfile(userId);
        System.out.println("用户ID: " + profile.getId());
        System.out.println("用户名: " + profile.getName());
        System.out.println("用户头像: " + profile.getAvatarUrl());
        System.out.println("视频数量: " + profile.getVideoCount());
        System.out.println("订阅者数量: " + profile.getSubscriberCount());
        System.out.println();
    }

    @Test
    public void testGetUploadVideos() throws Exception {
        System.out.println("--- 获取用户上传视频 ---");
        CommonParam param = CommonParam.builder()
                .code("367290")
                .page(2)
                .sort("oldest")
                .build();
        List<VideoInfo> videos = client.getUploadVideos(param);
        System.out.println("上传视频数量: " + videos.size());
        for (int i = 0; i < Math.min(3, videos.size()); i++) {
            VideoInfo info = videos.get(i);
            System.out.println("  [" + (i + 1) + "] " + info.getTitle() + " | 代码: " + info.getVideoCode());
        }
        System.out.println();
    }

    @Test
    public void testGetPlaylists() throws Exception {
        System.out.println("--- 获取用户播放列表 ---");
        CommonParam param = CommonParam.builder()
                .code("367290")
                .page(1)
                .sort("oldest")
                .build();
        List<PlaylistItem> playlists = client.getPlaylists(param);
        System.out.println("播放列表数量: " + playlists.size());
        for (PlaylistItem item : playlists) {
            System.out.println("  " + item.getTitle() + " | 代码: " + item.getListCode() + " | 链接: " + item.getListUrl());
            System.out.println("  total: " + item.getTotal());
        }
        System.out.println();
    }

    @Test
    public void testGetUserPage() throws Exception {
        System.out.println("--- 获取用户页面 ---");
        String userId = "995886";
        UserPage userPage = client.getUserPage(userId);
        Profile profile = userPage.getProfile();
        System.out.println("用户ID: " + profile.getId());
        System.out.println("用户名: " + profile.getName());
        System.out.println("用户头像: " + profile.getAvatarUrl());
        System.out.println("用户视频数量: " + profile.getVideoCount());
        System.out.println("用户订阅数量: " + profile.getSubscriberCount());
        System.out.println("------------ 用户视频列表 ------------");
        List<VideoInfo> videoList = userPage.getVideoList();
        if (videoList != null) {
            for (VideoInfo videoInfo : videoList) {
                System.out.println("  " + videoInfo.getTitle() + " | 代码: " + videoInfo.getVideoCode());
            }
        }
        System.out.println("------------ 用户播放列表 ------------");
        List<PlaylistItem> playlist = userPage.getPlaylists();
        if (playlist != null) {
            for (PlaylistItem playlistItem : playlist) {
                System.out.println("  " + playlistItem.getTitle() + " | 代码: " + playlistItem.getListCode() + " | 链接: " + playlistItem.getListUrl());
                System.out.println("total: " + playlistItem.getTotal());
            }
        }
    }

    @Test
    public void testGetUserUploadedPage() throws Exception {
        System.out.println("--- 获取用户上传视频页面 ---");
        String userId = "367290";
        CommonParam userParam = CommonParam.builder()
                .code(userId)
                .page(2)
                .sort("oldest")
                .build();
        UserUploadedPage userUploadedPage = client.getUserUploadedPage(userParam);
        Profile profile = userUploadedPage.getProfile();
        System.out.println("用户ID: " + profile.getId());
        System.out.println("用户名: " + profile.getName());
        System.out.println("用户头像: " + profile.getAvatarUrl());
        System.out.println("用户视频数量: " + profile.getVideoCount());
        System.out.println("用户订阅数量: " + profile.getSubscriberCount());
        System.out.println("------------ 用户视频列表 ------------");
        List<VideoInfo> videoList = userUploadedPage.getVideoList();
        if (videoList != null) {
            for (VideoInfo videoInfo : videoList) {
                System.out.println("  " + videoInfo.getTitle() + " | 代码: " + videoInfo.getVideoCode());
            }
        }
    }

    @Test
    public void testGetUserPlaylistsPage() throws Exception {
        System.out.println("--- 获取用户播放列表页面 ---");
        String userId = "367290";
        CommonParam userParam = CommonParam.builder()
                .code(userId)
                .page(1)
                .sort("oldest")
                .build();
        UserPlaylistsPage userPlaylistsPage = client.getUserPlaylistsPage(userParam);
        Profile profile = userPlaylistsPage.getProfile();
        System.out.println("用户ID: " + profile.getId());
        System.out.println("用户名: " + profile.getName());
        System.out.println("用户头像: " + profile.getAvatarUrl());
        System.out.println("用户视频数量: " + profile.getVideoCount());
        System.out.println("用户订阅数量: " + profile.getSubscriberCount());
        System.out.println("------------ 用户播放列表 ------------");
        List<PlaylistItem> playlist = userPlaylistsPage.getPlaylists();
        if (playlist != null) {
            for (PlaylistItem playlistItem : playlist) {
                System.out.println("  " + playlistItem.getTitle() + " | 代码: " + playlistItem.getListCode() + " | 链接: " + playlistItem.getListUrl());
                System.out.println("total: " + playlistItem.getTotal());
            }
        }
    }

    @Test
    public void testGetPlaylist() throws Exception {
        System.out.println("--- 获取播放列表页面 ---");
        String listCode = "685024";
        CommonParam listParam = CommonParam.builder()
                .code(listCode)
                .page(1)
                .build();
        Playlist playlistsPage = client.getPlaylist(listParam);
        System.out.println("播放列表名称: " + playlistsPage.getTitle());
        System.out.println("播放列表描述: " + playlistsPage.getDescription());
        System.out.println("播放列表封面: " + playlistsPage.getCoverUrl());
        System.out.println("播放列表视频数量: " + playlistsPage.getTotal());
        System.out.println("------------ 播放列表视频 ------------");
        List<VideoInfo> videoList = playlistsPage.getVideos();
        if (videoList != null) {
            for (VideoInfo videoInfo : videoList) {
                System.out.println("  " + videoInfo.getTitle() + " | 代码: " + videoInfo.getVideoCode());
            }
        }
    }

    @Test
    public void testDownloadInfo() throws Exception {
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
    public void testGetGenreList() throws Exception {
        List<String> genreList = client.getGenreList();
        System.out.println("类型大小: " + genreList.size());
        for (String genre : genreList) {
            System.out.println("类型：" + genre);
        }
    }

    @Test
    public void testGetTagsMap() throws Exception {
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
    public void testGetSortTypeList() throws Exception {
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
    public void testGetComments() throws Exception {
        System.out.println("--- 获取视频评论 ---");
        String videoCode = "405939";
        List<Comment> comments = client.getComments("video", videoCode);
        System.out.println("评论总数: " + comments.size());
        for (int i = 0; i < Math.min(5, comments.size()); i++) {
            Comment c = comments.get(i);
            System.out.println("  [" + (i + 1) + "] " + c.getUsername() + " | " + c.getTime());
            System.out.println("      ID: " + c.getId());
            System.out.println("      内容: " + (c.getText() != null ? c.getText().substring(0, Math.min(50, c.getText().length())) : "null"));
            System.out.println("      头像: " + c.getAvatarUrl());
            System.out.println("      点赞: " + c.getLikes() + " | 踩: " + c.getDislikes() + " | 回复: " + c.getReplyCount());
            if (!comments.isEmpty()) {
                System.out.println("第一条评论完整信息: " + comments.get(0));
            }
            System.out.println();
        }
    }

    @Test
    public void testGetVideoComments() throws Exception {
        System.out.println("--- 获取影片评论（便捷方法） ---");
        String videoCode = "405939";
        List<Comment> comments = client.getVideoComments(videoCode);
        System.out.println("评论总数: " + comments.size());
        for (int i = 0; i < Math.min(3, comments.size()); i++) {
            Comment c = comments.get(i);
            System.out.println("  [" + (i + 1) + "] " + c.getUsername() + ": "
                    + (c.getText() != null ? c.getText().substring(0, Math.min(40, c.getText().length())) : "null"));
        }
        System.out.println();
    }

    @Test
    public void testGetReplies() throws Exception {
        System.out.println("--- 获取评论回复 ---");
        // 先获取评论列表，取第一条评论的ID来查询回复
        String videoCode = "405939";
        List<Comment> comments = client.getVideoComments(videoCode);
        if (comments.isEmpty()) {
            System.out.println("没有评论，跳过回复测试");
            System.out.println();
            return;
        }
        String commentId = comments.get(0).getId();
        System.out.println("使用评论ID: " + commentId + " (来自: " + comments.get(0).getUsername() + ")");
        List<Comment> replies = client.comment().getReplies(commentId);
        System.out.println("回复总数: " + replies.size());
        for (int i = 0; i < Math.min(3, replies.size()); i++) {
            Comment r = replies.get(i);
            System.out.println("  [" + (i + 1) + "] " + r.getUsername() + " | " + r.getTime());
            System.out.println("      ID: " + r.getId());
            System.out.println("      内容: " + (r.getText() != null ? r.getText().substring(0, Math.min(50, r.getText().length())) : "null"));
            System.out.println("      点赞: " + r.getLikes() + " | 踩: " + r.getDislikes());
        }
        System.out.println();
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
