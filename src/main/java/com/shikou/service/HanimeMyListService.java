package com.shikou.service;

import com.shikou.config.HanimeConfig;
import com.shikou.model.VideoInfo;
import com.shikou.model.PlaylistItem;
import com.shikou.parser.HtmlParser;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

/**
 * 我的列表服务 - 播放列表管理、收藏
 */
public class HanimeMyListService {

    private final OkHttpClient client;
    private final HanimeConfig config;

    public HanimeMyListService(OkHttpClient client, HanimeConfig config) {
        this.client = client;
        this.config = config;
    }

    // ======================== 获取播放列表 ========================

    /**
     * 获取播放列表内容
     * @param page 页码，从1开始
     * @param listType 列表类型: "WL"(稍后观看), "LL"(喜欢的影片), "SL"(订阅), 或自定义列表代码
     */
    public List<VideoInfo> getPlaylist(int page, String listType) throws IOException {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "playlist").newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("list", listType)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("获取播放列表失败: " + response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            return HtmlParser.parseVideoCards(doc);
        }
    }

    public List<VideoInfo> getWatchLater(int page) throws IOException {
        return getPlaylist(page, "WL");
    }

    public List<VideoInfo> getLikedVideos(int page) throws IOException {
        return getPlaylist(page, "LL");
    }

    public List<VideoInfo> getSubscriptions(int page) throws IOException {
        return getPlaylist(page, "SL");
    }

    // ======================== 获取所有播放列表 ========================

    public List<PlaylistItem> getAllPlaylists() throws IOException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "playlists")
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("获取播放列表失败: " + response.code());
            }
            String html = response.body().string();
            var doc = org.jsoup.Jsoup.parse(html, config.getBaseUrl());
            return HtmlParser.parsePlaylistItems(doc);
        }
    }

    // ======================== 删除列表项 ========================

    public boolean deletePlaylistItem(String csrfToken, String playlistId, String videoId) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("playlist_id", playlistId)
                .add("video_id", videoId)
                .add("count", "1")
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "deletePlayitem")
                .post(formBody)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== 收藏/取消收藏 ========================

    public boolean toggleFavorite(String csrfToken, String videoCode, boolean add, String userId) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("like-foreign-id", videoCode)
                .add("like-status", add ? "" : "1")
                .add("_token", csrfToken)
                .add("like-is-positive", "1");

        if (userId != null) {
            formBuilder.add("like-user-id", userId);
        }

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "like")
                .post(formBuilder.build())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean addFavorite(String csrfToken, String videoCode, String userId) throws IOException {
        return toggleFavorite(csrfToken, videoCode, true, userId);
    }

    public boolean removeFavorite(String csrfToken, String videoCode, String userId) throws IOException {
        return toggleFavorite(csrfToken, videoCode, false, userId);
    }

    // ======================== 创建播放列表 ========================

    public boolean createPlaylist(String csrfToken, String videoId, String title, String description) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("create-playlist-video-id", videoId)
                .add("playlist-title", title)
                .add("playlist-description", description)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "createPlaylist")
                .post(formBody)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== 添加到播放列表 ========================

    public boolean saveToPlaylist(String csrfToken, String listCode, String videoId, String userId) throws IOException {
        return saveToPlaylist(csrfToken, listCode, videoId, true, userId);
    }

    public boolean removeFromPlaylist(String csrfToken, String listCode, String videoId, String userId) throws IOException {
        return saveToPlaylist(csrfToken, listCode, videoId, false, userId);
    }

    private boolean saveToPlaylist(String csrfToken, String listCode, String videoId, boolean add, String userId) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("input_id", listCode)
                .add("video_id", videoId)
                .add("is_checked", String.valueOf(add));

        if (userId != null) {
            formBuilder.add("user_id", userId);
        }

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "save")
                .post(formBuilder.build())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== 修改播放列表 ========================

    public boolean modifyPlaylist(String csrfToken, String listCode, String title,
                                   String description, boolean delete) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("_method", "PUT")
                .add("playlist-title", title)
                .add("playlist-description", description);

        if (delete) {
            formBuilder.add("playlist-delete", "on");
        }

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "playlist/" + listCode)
                .post(formBuilder.build())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean deletePlaylist(String csrfToken, String listCode) throws IOException {
        return modifyPlaylist(csrfToken, listCode, "", "", true);
    }
}
