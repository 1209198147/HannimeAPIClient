package com.shikou.service;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.*;
import com.shikou.model.*;
import com.shikou.parser.HtmlParser;
import okhttp3.*;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;

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
    public HomePage getHomePage() throws HanimeException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("获取首页失败", response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            HomePage homePage = HtmlParser.parseHomePage(doc);
            return homePage;
        }catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    // ======================== 搜索 ========================

    /**
     * 搜索影片
     * @param params 搜索参数
     * @return 影片列表
     */
    public List<VideoInfo> search(SearchParams params) throws HanimeException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(config.getBaseUrl() + "search").newBuilder();

        if (params.getPage() > 0) {
            urlBuilder.addQueryParameter("page", String.valueOf(params.getPage()));
        }
        if (params.getQuery() != null && !params.getQuery().isEmpty()) {
            urlBuilder.addQueryParameter("query", params.getQuery());
        }
        if (params.getGenre() != null && !params.getGenre().isEmpty()) {
            urlBuilder.addQueryParameter("genre", params.getGenre());
        }
        if (params.getSort() != null && !params.getSort().isEmpty()) {
            urlBuilder.addQueryParameter("sort", params.getSort());
        }
        if (params.getBroad() != null && !params.getBroad().isEmpty()) {
            urlBuilder.addQueryParameter("broad", params.getBroad());
        }
        if (params.getYear() != null) {
            urlBuilder.addQueryParameter("year", String.valueOf(params.getYear()));
        }
        if (params.getMonth() != null) {
            urlBuilder.addQueryParameter("month", String.valueOf(params.getMonth()));
        }
        if (params.getDuration() != null && !params.getDuration().isEmpty()) {
            urlBuilder.addQueryParameter("duration", params.getDuration());
        }
        if (params.getTags() != null) {
            for (String tag : params.getTags()) {
                urlBuilder.addQueryParameter("tags[]", tag);
            }
        }
        if (params.getBrands() != null) {
            for (String brand : params.getBrands()) {
                urlBuilder.addQueryParameter("brands[]", brand);
            }
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("搜索失败", response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            return HtmlParser.parseSearchResults(doc);
        } catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    /**
     * 简单搜索（仅关键词）
     * @param query 搜索关键词
     * @return 影片列表
     */
    public List<VideoInfo> search(String query) throws HanimeException {
        return search(new SearchParams(query));
    }

    // ======================== 影片详情 ========================

    /**
     * 获取影片详情
     * @param videoCode 影片代码
     * @return HanimeVideo 影片详情
     */
    public HanimeVideo getVideoDetail(String videoCode) throws HanimeException {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "watch").newBuilder()
                .addQueryParameter("v", videoCode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("获取影片详情失败", response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            HanimeVideo video = HtmlParser.parseVideoDetail(doc);
            return video;
        } catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    // ======================== 下载页 ========================

    /**
     * 获取下载页面
     * @param videoCode 影片代码
     * @return DownloadPage 下载页面数据
     */
    public DownloadInfo getDownloadInfo(String videoCode) throws HanimeException {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "download").newBuilder()
                .addQueryParameter("v", videoCode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("获取下载页面失败", response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            return HtmlParser.parseDownloadPage(doc);
        } catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    // ======================== 预览 ========================

    /**
     * 获取预览页面
     * @param date 日期，格式如 202206、202012
     * @return PreviewPage 预览页面数据
     */
    public PreviewPage getPreviews(String date) throws HanimeException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "previews/" + date)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("获取预览页面失败", response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            return HtmlParser.parsePreviewPage(doc);
        }catch (IOException e){
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    // ======================== 登录 ========================

    /**
     * 获取登录页面（提取CSRF Token）
     * @return CSRF Token
     */
    public String getCsrfToken() throws HanimeException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "login")
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
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
    public String login(String email, String password) throws HanimeException {
        String csrfToken = getCsrfToken();

        if (csrfToken == null || csrfToken.isEmpty()) {
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
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl() + "login")
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
    public boolean verifyLogin() throws HanimeException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "login")
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Cookie", "user_lang=" + config.getUserLang())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.code() == 404;
        }catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }
}
