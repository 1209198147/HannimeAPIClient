package com.shikou.service;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.*;
import com.shikou.model.entities.*;
import com.shikou.model.entities.pages.*;
import com.shikou.model.entities.results.PlaylistsResult;
import com.shikou.model.entities.results.VideosResult;
import com.shikou.parser.HtmlParser;
import com.shikou.util.HanimeHttpExecutor;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基础服务 - 首页、搜索、影片详情、预览、登录
 */
public class HanimeBaseService {

    private final OkHttpClient client;
    private final HanimeConfig config;

    public HanimeBaseService(OkHttpClient client, HanimeConfig config) {
        this.client = client;
        this.config = config;
    }

    // ======================== 首页 ========================

    /**
     * 获取首页
     * @return HomePage 首页数据
     */
    public HomePage getHomePage() throws HanimeApiException, HanimeNetworkException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl())
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取首页失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseHomePage(doc);
    }

    // ======================== 搜索 ========================

    /**
     * 搜索影片
     * @param params 搜索参数
     * @return 影片列表
     */
    public VideosResult search(SearchParams params) throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getSearchUrl(params);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "搜索失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseSearchResults(doc);
    }

    private HttpUrl getSearchUrl() {
        return getSearchUrl(null);
    }

    private HttpUrl getSearchUrl(SearchParams params) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "search")).newBuilder();

        if(params == null){
            return urlBuilder.build();
        }

        if (params.getPage() > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(params.getPage()));
        }
        if (StringUtils.isNotEmpty(params.getQuery())) {
            urlBuilder.addQueryParameter("query", params.getQuery());
        }
        if (StringUtils.isNotEmpty(params.getGenre())) {
            urlBuilder.addQueryParameter("genre", params.getGenre());
        }
        if (CollectionUtils.isNotEmpty(params.getTags())) {
            for (String tag : params.getTags()) {
                urlBuilder.addQueryParameter("tags[]", tag);
            }
        }
        if (StringUtils.isNotEmpty(params.getSort())) {
            urlBuilder.addQueryParameter("sort", params.getSort());
        }
        if(StringUtils.isNotEmpty(params.getDate())){
            urlBuilder.addQueryParameter("date", params.getDate());
        }
        if (StringUtils.isNotEmpty(params.getDuration())) {
            urlBuilder.addQueryParameter("duration", params.getDuration());
        }
        return urlBuilder.build();
    }

    /**
     * 简单搜索（仅关键词）
     * @param query 搜索关键词
     * @return 影片列表
     */
    public VideosResult search(String query) throws HanimeApiException, HanimeNetworkException {
        return search(new SearchParams(query));
    }

    // ======================== 影片详情 ========================

    /**
     * 获取影片详情
     * @param videoCode 影片代码
     * @return HanimeVideo 影片详情
     */
    public HanimeVideo getVideoDetail(String videoCode) throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getWatchUrl(videoCode);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取影片详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseVideoDetail(doc);
    }

    // ======================== 下载页 ========================

    /**
     * 获取下载页面
     * @param videoCode 影片代码
     * @return DownloadPage 下载页面数据
     */
    public DownloadInfo getDownloadInfo(String videoCode) throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getDownloadInfoUrl(videoCode);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取下载页面失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseDownloadPage(doc);
    }

    @NotNull
    private HttpUrl getDownloadInfoUrl(String videoCode) {
        return Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "download")).newBuilder()
                .addQueryParameter("v", videoCode)
                .build();
    }

    // ======================== 预览 ========================

    /**
     * 获取预览页面
     * @param date 日期，格式如 202206、202012
     * @return PreviewPage 预览页面数据
     */
    public PreviewPage getPreviews(String date) throws HanimeApiException, HanimeNetworkException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "previews/" + date)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取预览页面失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parsePreviewPage(doc);
    }

    // ======================== 登录 ========================

    /**
     * 获取登录页面（提取CSRF Token）
     * @return CSRF Token
     */
    public String getCsrfToken() throws HanimeApiException, HanimeNetworkException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "login")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                throw new HanimeApiException("获取登录页面失败");
            }
            String html = response.body().string();
            var doc = Jsoup.parse(html, config.getBaseUrl());
            return null;
        } catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param email 邮箱
     * @param password 密码
     * @return 是否登录成功
     */
    public String login(String email, String password) throws HanimeApiException, HanimeNetworkException, HanimeAuthenticationException {
        String csrfToken = getCsrfToken();

        if (StringUtils.isEmpty(csrfToken)) {
            throw new HanimeAuthenticationException("无法获取CSRF Token");
        }

        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "login")
                .post(formBody)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            boolean success = verifyLogin();
            if (success) {
                return csrfToken;
            }
            return null;
        }catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    /**
     * 验证是否已登录
     * @return 是否已登录
     */
    public boolean verifyLogin() throws HanimeNetworkException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "login")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.code() == 404;
        }catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    public Map<String, String> getGenreList() throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getSearchUrl();

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取类型列表失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseGenreList(doc);
    }

    public Map<String, Map<String, String>> getTagsMap() throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getSearchUrl();

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取标签列表失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseTagsMap(doc);
    }

    public Map<String, String> getSortTypeList() throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getSearchUrl();

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取排序方式列表失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseSortTypeList(doc);
    }

    public SearchPage getSearchPage(SearchParams params) throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getSearchUrl(params);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取搜索页面失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseSearchPage(doc);
    }

    public WatchPage getWatchPage(String videoCode) throws HanimeApiException, HanimeNetworkException {
        HttpUrl url = getWatchUrl(videoCode);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取影片详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseWatchPage(doc);
    }

    @NotNull
    private HttpUrl getWatchUrl(String videoCode) {
        return Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "watch")).newBuilder()
                .addQueryParameter("v", videoCode)
                .build();
    }

    @NotNull
    private HttpUrl getUserUrl(String userId) {
        return Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "user/" + userId)).newBuilder()
                .build();
    }

    @NotNull
    private HttpUrl getUserUploadedUrl(CommonParam param) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "user/" + param.getCode() + "/uploaded")).newBuilder()
                .addQueryParameter("page", String.valueOf(param.getPage()));

        if(StringUtils.isNotBlank(param.getSort())){
            builder.addQueryParameter("sort", param.getSort());
        }

        return builder.build();
    }

    @NotNull
    private HttpUrl getUserPlaylistsUrl(CommonParam param) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "user/" + param.getCode() + "/playlists")).newBuilder()
                .addQueryParameter("page", String.valueOf(param.getPage()));

        if(StringUtils.isNotBlank(param.getSort())){
            builder.addQueryParameter("sort", param.getSort());
        }

        return builder.build();
    }



    public UserPage getUserPage(String userId) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserUrl(userId);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取用户详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseUserPage(doc);
    }

    public UserUploadedPage getUserUploadedPage(CommonParam param) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserUploadedUrl(param);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取用户上传视频页失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseUserUploadedPage(doc);
    }

    public UserPlaylistsPage getUserPlaylistsPage(CommonParam param) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserPlaylistsUrl(param);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取用户上传视频页失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseUserPlaylistsPage(doc);
    }

    public Profile getProfile(String userId) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserUrl(userId);
        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取用户详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseProfile(doc);
    }

    public VideosResult getUploadVideos(CommonParam param) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserUploadedUrl(param);
        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取" + param.getCode() + "上传视频列表详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parseUploadVideos(doc);
    }

    public PlaylistsResult getPlaylists(CommonParam param) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getUserPlaylistsUrl(param);
        Request request = new Request.Builder()
                .url(url)
                .build();

        String html = HanimeHttpExecutor.executeForString(client, request, "获取" + param.getCode() + "获取用户播放列表详情失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parsePlaylists(doc);
    }

    public Playlist getPlaylist(CommonParam param) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getPlaylistUrl(param);
        Request request = new Request.Builder()
                .url(url)
                .build();
        String html = HanimeHttpExecutor.executeForString(client, request, "获取播放列表页失败");
        var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
        return HtmlParser.parsePlaylistPage(doc);
    }

    @NotNull
    private HttpUrl getPlaylistUrl(CommonParam param) {
        return HttpUrl.parse(config.getBaseUrl() + "playlist").newBuilder()
                .addQueryParameter("list", param.getCode())
                .addQueryParameter("page", String.valueOf(param.getPage())).build();
    }
}
