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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    public void download(String videoCode, File outputFile) throws HanimeException, IOException {
        HanimeVideo video = baseService.getVideoDetail(videoCode);
        if (video.getVideoUrls() == null || video.getVideoUrls().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality quality = selectBestQuality(video.getVideoUrls());
        videoDownloader.download(quality.getUrl(), outputFile);
    }

    public void download(String videoCode, String quality, File outputFile) throws HanimeException, IOException {
        HanimeVideo video = baseService.getVideoDetail(videoCode);
        if (video.getVideoUrls() == null || video.getVideoUrls().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality vq = video.getVideoUrls().get(quality);
        if (vq == null) {
            vq = selectBestQuality(video.getVideoUrls());
        }
        videoDownloader.download(vq.getUrl(), outputFile);
    }

    public void download(String videoCode, String quality, File outputFile,
                          VideoDownloader.ProgressListener listener) throws HanimeException, IOException {
        HanimeVideo video = baseService.getVideoDetail(videoCode);
        if (video.getVideoUrls() == null || video.getVideoUrls().isEmpty()) {
            throw new HanimeApiException("未找到视频URL");
        }
        VideoQuality vq = video.getVideoUrls().get(quality);
        if (vq == null) {
            vq = selectBestQuality(video.getVideoUrls());
        }
        videoDownloader.download(vq.getUrl(), outputFile, listener);
    }

    private VideoQuality selectBestQuality(java.util.Map<String, VideoQuality> videoUrls) {
        String[] priorities = {"1080P", "720P", "480P", "240P", "Unknown"};
        for (String p : priorities) {
            VideoQuality vq = videoUrls.get(p);
            if (vq != null) {
                return vq;
            }
        }
        return videoUrls.values().iterator().next();
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
