package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeAuthenticationException;
import com.shikou.exception.HanimeException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.model.entities.*;
import com.shikou.model.entities.page.*;
import com.shikou.service.*;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    public DownloadInfo getDownloadInfo(String videoCode) throws HanimeApiException, HanimeNetworkException {
        return baseService.getDownloadInfo(videoCode);
    }

    public HomePage getHomePage() throws HanimeApiException, HanimeNetworkException {
        return baseService.getHomePage();
    }

    public SearchPage getSearchPage(SearchParams searchParams) throws HanimeApiException, HanimeNetworkException {
        return baseService.getSearchPage(searchParams);
    }

    public WatchPage getWatchPage(String videoCode) throws HanimeApiException, HanimeNetworkException {
        return baseService.getWatchPage(videoCode);
    }

    public List<Comment> getComments(String type, String code) throws HanimeApiException, HanimeNetworkException {
        return commentService.getComments(type, code);
    }

    public List<Comment> getVideoComments(String videoCode) throws HanimeApiException, HanimeNetworkException {
        return commentService.getVideoComments(videoCode);
    }

    public List<Comment> getReplies(String commentId) throws HanimeApiException, HanimeNetworkException {
        return commentService.getReplies(commentId);
    }

    public UserPage getUserPage(String userId) throws HanimeApiException, HanimeNetworkException {
        return baseService.getUserPage(userId);
    }

    public UserUploadedPage getUserUploadedPage(String userId) throws HanimeApiException, HanimeNetworkException {
        CommonParam param = CommonParam.builder().code(userId).build();
        return getUserUploadedPage(param);
    }

    public UserUploadedPage getUserUploadedPage(CommonParam param) throws HanimeApiException, HanimeNetworkException {
        return baseService.getUserUploadedPage(param);
    }

    public UserPlaylistsPage getUserPlaylistsPage(String userId) throws HanimeApiException, HanimeNetworkException {
        CommonParam param = CommonParam.builder().code(userId).build();
        return getUserPlaylistsPage(param);
    }

    public UserPlaylistsPage getUserPlaylistsPage(CommonParam param) throws HanimeApiException, HanimeNetworkException {
        return baseService.getUserPlaylistsPage(param);
    }

    public List<String> getGenreList() throws HanimeApiException, HanimeNetworkException {
        return baseService.getGenreList();
    }

    public Map<String, List<String>> getTagsMap() throws HanimeApiException, HanimeNetworkException {
        return baseService.getTagsMap();
    }

    public List<String> getSortTypeList() throws HanimeApiException, HanimeNetworkException {
        return baseService.getSortTypeList();
    }

    public SearchResult search(String query) throws HanimeApiException, HanimeNetworkException {
        return baseService.search(query);
    }

    public SearchResult search(SearchParams params) throws HanimeApiException, HanimeNetworkException {
        return baseService.search(params);
    }

    public HanimeVideo getVideoDetail(String videoCode) throws HanimeApiException, HanimeNetworkException {
        return baseService.getVideoDetail(videoCode);
    }

    public Profile getProfile(String userId) throws HanimeApiException, HanimeNetworkException {
        return baseService.getProfile(userId);
    }

    public List<VideoInfo> getUploadVideos(CommonParam param) throws HanimeApiException, HanimeNetworkException {
        return baseService.getUploadVideos(param);
    }

    public List<PlaylistItem> getPlaylists(CommonParam param) throws HanimeApiException, HanimeNetworkException {
        return baseService.getPlaylists(param);
    }

    public Playlist getPlaylist(CommonParam param) throws HanimeApiException, HanimeNetworkException {
        return baseService.getPlaylist(param);
    }

    public PreviewPage getPreviews(String date) throws HanimeApiException, HanimeNetworkException {
        return baseService.getPreviews(date);
    }

    public boolean login(String email, String password) throws HanimeApiException, HanimeNetworkException, HanimeAuthenticationException {
        String csrfToken = baseService.login(email, password);
        this.csrfToken = csrfToken;
        return StringUtils.isNotEmpty(csrfToken);
    }

    public boolean isLoggedIn() throws HanimeNetworkException {
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
    public void download(String videoCode, File outputFile) throws HanimeApiException, HanimeNetworkException, IOException {
        download(videoCode, outputFile, null);
    }

    /**
     * 下载视频 - 最高画质（带进度监听）
     *
     * @param videoCode 视频代码
     * @param outputFile
     * @throws HanimeException
     * @throws IOException
     */
    public void download(String videoCode, File outputFile, VideoDownloader.ProgressListener listener) throws HanimeApiException, HanimeNetworkException, IOException {
        String url = resolveDownloadUrl(videoCode, null);
        videoDownloader.download(url, outputFile, listener);
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
    public void download(String videoCode, String quality, File outputFile) throws HanimeApiException, HanimeNetworkException, IOException {
        String url = resolveDownloadUrl(videoCode, quality);
        videoDownloader.download(url, outputFile);
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
                          VideoDownloader.ProgressListener listener) throws HanimeApiException, HanimeNetworkException, IOException {
        String url = resolveDownloadUrl(videoCode, quality);
        videoDownloader.download(url, outputFile, listener);
    }

    /**
     * 下载视频 - 指定画质（带进度监听）
     *
     * @param args
     * @param listener
     * @throws HanimeException
     * @throws IOException
     */
    public void download(DownloadArgs args,
                         VideoDownloader.ProgressListener listener) throws HanimeApiException, HanimeNetworkException, IOException {
        if (!vaildDownloadArgs(args)){
            return;
        }

        File outputFile = new File(args.getDownloadDir(), args.getVideoName() + "." + args.getSuffix());
        String videoCode = args.getVideoCode();
        String quality = args.getQuality();
        if(StringUtils.isNotEmpty(videoCode)) {
            if(StringUtils.isNotEmpty(quality)) {
                download(videoCode, quality, outputFile, listener);
            }else{
                download(videoCode, outputFile, listener);
            }
            return;
        }
        String downloadUrl = args.getDownloadUrl();
        if(StringUtils.isNotEmpty(downloadUrl)) {
            videoDownloader.download(downloadUrl, outputFile, listener);
        }
        String coverUrl = args.getCoverUrl();
        if(StringUtils.isNotEmpty(coverUrl)) {
            File coverFile = new File(args.getDownloadDir(), "cover_" + args.getVideoName() + ".png");
            videoDownloader.download(coverUrl, coverFile, listener);
        }
    }

    private static boolean vaildDownloadArgs(DownloadArgs args) throws HanimeApiException {
        if(args == null) {
            return false;
        }

        if(StringUtils.isAllEmpty(args.getVideoCode(), args.getDownloadDir())) {
            throw new HanimeApiException("视频码和下载链接不能都为空");
        }

        if(StringUtils.isEmpty(args.getVideoName())) {
            throw new HanimeApiException("视频名称不能为空");
        }

        if(StringUtils.isEmpty(args.getSuffix())) {
            args.setSuffix("mp4");
        }

        if(args.getSuffix().startsWith(".")){
            args.setSuffix(args.getSuffix().substring(1));
        }
        return true;
    }

    /**
     * 解析下载URL（根据画质选择或自动选择最佳画质）
     */
    private String resolveDownloadUrl(String videoCode, String quality) throws HanimeApiException, HanimeNetworkException {
        DownloadInfo downloadInfo = baseService.getDownloadInfo(videoCode);
        if (CollectionUtils.isEmpty(downloadInfo.getDownloadItems())) {
            throw new HanimeApiException("未找到视频URL");
        }
        if (StringUtils.isNotEmpty(quality)) {
            String url = getQualityUrl(quality, downloadInfo);
            if (StringUtils.isEmpty(url) || !url.startsWith("http")) {
                throw new HanimeApiException("无法获取视频的下载链接");
            }
            return url;
        }
        String bestQualityUrl = selectBestQuality(downloadInfo.getDownloadItems());
        if (StringUtils.isBlank(bestQualityUrl)) {
            throw new HanimeApiException("无法获取视频的下载链接");
        }
        return bestQualityUrl;
    }


    private String getQualityUrl(String quality, DownloadInfo downloadInfo){
        Map<String, String> qualityUrlMap = downloadInfo.getDownloadItems().stream()
                .collect(Collectors.toMap(item -> item.getQuality().toUpperCase(), item -> item.getDownloadUrl()));
        return qualityUrlMap.get(quality);
    }

    /**
     * 选择最佳质量的视频
     */
    private String selectBestQuality(List<DownloadItem> downloadItems) {
        if(CollectionUtils.isEmpty(downloadItems)){
            return null;
        }

        Map<String, String> qualityUrlMap = downloadItems.stream()
                .filter(item -> !StringUtils.isAnyBlank(item.getDownloadUrl(), item.getQuality()))
                .collect(Collectors.toMap(DownloadItem::getQuality, DownloadItem::getDownloadUrl));

        if(MapUtils.isEmpty(qualityUrlMap)){
            return null;
        }
        List<Map.Entry<String, String>> qualityUrlList = new ArrayList<>(qualityUrlMap.entrySet());

        qualityUrlList.sort((o1, o2) -> {
            String k1 = o1.getKey();
            String k2 = o2.getKey();
            int q1 = Integer.parseInt(k1.substring(0, Math.min(k1.length(), k1.length() - 2)));
            int q2 = Integer.parseInt(k2.substring(0, Math.min(k2.length(), k2.length() - 2)));
            return q2 - q1;
        });
        return qualityUrlList.getFirst().getValue();
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
