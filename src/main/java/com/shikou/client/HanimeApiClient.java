package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeException;
import com.shikou.model.*;
import com.shikou.service.*;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Hanime API 客户端 - 统一入口
 *
 * <p>使用方式:</p>
 * <pre>
 *     HanimeApiClient client = new HanimeApiClient();
 *
 *     // 获取首页
 *     HomePage home = client.getHomePage();
 *
 *     // 搜索影片
 *     List&lt;VideoInfo&gt; results = client.search("关键词");
 *
 *     // 获取影片详情
 *     HanimeVideo video = client.getVideoDetail("videoCode");
 *
 *     // 登录
 *     boolean success = client.login("email", "password");
 *
 *     // 下载视频
 *     client.download(videoCode, "1080P", new File("output.mp4"));
 * </pre>
 */
public class HanimeApiClient {

    private final OkHttpClient httpClient;
    private final HanimeConfig config;

    /** 用户会话级CSRF Token，所有Service共享 */
    private String csrfToken;

    // 服务实例
    private final HanimeBaseService baseService;
    private final HanimeCommentService commentService;
    private final HanimeMyListService myListService;
    private final HanimeSubscriptionService subscriptionService;
    private final VideoDownloader videoDownloader;

    // ======================== 构造方法 ========================

    /**
     * 使用默认配置创建客户端
     */
    public HanimeApiClient() {
        this(HanimeConfig.defaultConfig());
    }

    /**
     * 使用自定义配置创建客户端
     * @param config 自定义配置
     */
    public HanimeApiClient(HanimeConfig config) {
        this(config, buildHttpClient(config));
    }

    /**
     * 使用自定义OkHttpClient创建客户端
     * @param httpClient 自定义OkHttpClient
     */
    public HanimeApiClient(OkHttpClient httpClient) {
        this(HanimeConfig.defaultConfig(), httpClient);
    }

    /**
     * 完全自定义创建客户端
     * @param config 配置
     * @param httpClient OkHttpClient
     */
    public HanimeApiClient(HanimeConfig config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;

        this.baseService = new HanimeBaseService(httpClient, config);
        this.commentService = new HanimeCommentService(httpClient, config);
        this.myListService = new HanimeMyListService(httpClient, config);
        this.subscriptionService = new HanimeSubscriptionService(httpClient, config);
        this.videoDownloader = new VideoDownloader(httpClient, config);
    }

    // ======================== 服务入口 ========================

    /**
     * 获取基础服务（首页、搜索、详情、预览、登录）
     */
    public HanimeBaseService base() {
        return baseService;
    }

    /**
     * 获取评论服务（获取评论、发表评论、回复、点赞）
     */
    public HanimeCommentService comment() {
        return commentService;
    }

    /**
     * 获取列表服务（播放列表、收藏）
     */
    public HanimeMyListService myList() {
        return myListService;
    }

    /**
     * 获取订阅服务（订阅/取消订阅作者）
     */
    public HanimeSubscriptionService subscription() {
        return subscriptionService;
    }

    // ======================== 便捷方法 ========================

    /**
     * 获取视频下载信息
     *
     * @param videoCode
     * @return
     * @throws HanimeException
     */
    public DownloadInfo getDownloadInfo(String videoCode) throws HanimeException {
        return baseService.getDownloadInfo(videoCode);
    }

    public HomePage getHomePage() throws HanimeException {
        return baseService.getHomePage();
    }

    public List<VideoInfo> search(String query) throws HanimeException {
        return baseService.search(query);
    }

    public List<VideoInfo> search(SearchParams params) throws HanimeException {
        return baseService.search(params);
    }

    public HanimeVideo getVideoDetail(String videoCode) throws HanimeException {
        return baseService.getVideoDetail(videoCode);
    }

    public PreviewPage getPreviews(String date) throws HanimeException {
        return baseService.getPreviews(date);
    }

    public boolean login(String email, String password) throws HanimeException {
        String csrfToken = baseService.login(email, password);
        this.csrfToken = csrfToken;
        return csrfToken != null && !csrfToken.isEmpty();
    }

    public boolean isLoggedIn() throws HanimeException {
        return baseService.verifyLogin();
    }

    // ======================== 视频下载 ========================

    /**
     * 下载视频 - 最高画质
     *
     * @param videoCode 视频代码
     * @param outputFile
     * @throws HanimeException
     * @throws IOException
     */
    public void download(String videoCode, File outputFile) throws HanimeException, IOException {
        DownloadInfo downloadInfo = baseService.getDownloadInfo(videoCode);
        if (downloadInfo.getDownloadItems() == null || downloadInfo.getDownloadItems().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality quality = selectBestQuality(downloadInfo.getDownloadItems());
        if (quality == null) {
            throw new HanimeApiException("未找到视频的" + quality + "画质");
        }
        videoDownloader.download(quality.getUrl(), outputFile);
    }

    /**
     * 下载视频 - 指定画质
     *
     * @param videoCode
     * @param quality
     * @param outputFile
     * @throws HanimeException
     * @throws IOException
     */
    public void download(String videoCode, String quality, File outputFile) throws HanimeException, IOException {
        DownloadInfo downloadInfo = baseService.getDownloadInfo(videoCode);
        if (downloadInfo.getDownloadItems() == null || downloadInfo.getDownloadItems().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality vq = selectQuality(downloadInfo.getDownloadItems(), quality);
        if (vq == null) {
            throw new HanimeApiException("未找到视频的下载源");
        }
        videoDownloader.download(vq.getUrl(), outputFile);
    }

    /**
     * 下载视频 - 指定画质（带进度监听）
     *
     * @param videoCode
     * @param quality
     * @param outputFile
     * @param listener
     * @throws HanimeException
     * @throws IOException
     */
    public void download(String videoCode, String quality, File outputFile,
                          VideoDownloader.ProgressListener listener) throws HanimeException, IOException {
        DownloadInfo downloadInfo = baseService.getDownloadInfo(videoCode);
        if (downloadInfo.getDownloadItems() == null || downloadInfo.getDownloadItems().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality vq = selectQuality(downloadInfo.getDownloadItems(), quality);
        if (vq == null) {
            throw new HanimeApiException("未找到视频的" + quality + "画质");
        }
        videoDownloader.download(vq.getUrl(), outputFile, listener);
    }

    /**
     * 选择最佳质量的视频
     * @param downloadItems
     * @return
     */
    private VideoQuality selectBestQuality(List<DownloadItem> downloadItems) {
        return selectBestQuality(downloadItems, null);
    }

    /**
     * 选择最佳质量的视频
     * @param downloadItems
     * @param quality 如果存在则返回这个分辨率，如果没有则返回最好的一个
     * @return
     */
    private VideoQuality selectBestQuality(List<DownloadItem> downloadItems, String quality) {
        String[] priorities = {"1080P", "720P", "480P", "240P", "Unknown"};
        Map<String, VideoQuality> videoUrls = downloadItems.stream()
                .collect(Collectors.toMap(DownloadItem::getResolution, item -> new VideoQuality(item.getResolution(), item.getDownloadUrl(), item.getItemType())));

        if (videoUrls.containsKey(quality)){
            return videoUrls.get(quality);
        }

        for (String p : priorities) {
            VideoQuality item = videoUrls.get(p);
            if (item != null) {
                return item;
            }
        }
        VideoQuality defaultValue = videoUrls.values().iterator().next();
        return defaultValue;
    }

    /**
     * 选择最佳质量的视频
     * @param downloadItems
     * @param quality 如果存在则返回这个分辨率，如果没有则返回最好的一个
     * @return
     */
    private VideoQuality selectQuality(List<DownloadItem> downloadItems, String quality) {
        Map<String, VideoQuality> videoUrls = downloadItems.stream()
                .collect(Collectors.toMap(DownloadItem::getResolution, item -> new VideoQuality(item.getResolution(), item.getDownloadUrl(), item.getItemType())));
        return videoUrls.get(quality);
    }

    // ======================== 其他 ========================

    public String getCsrfToken() {
        return csrfToken;
    }

    public HanimeConfig getConfig() {
        return config;
    }

    // ======================== 静态工厂方法 ========================

    private static OkHttpClient buildHttpClient(HanimeConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getWriteTimeout(), TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // ======================== Builder ========================

    public static class Builder {
        private HanimeConfig config;
        private OkHttpClient httpClient;

        public Builder config(HanimeConfig config) {
            this.config = config;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public HanimeApiClient build() {
            if (config == null) {
                config = HanimeConfig.defaultConfig();
            }
            if (httpClient == null) {
                httpClient = buildHttpClient(config);
            }
            return new HanimeApiClient(config, httpClient);
        }
    }
}
