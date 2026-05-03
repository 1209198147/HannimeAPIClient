package com.shikou.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.model.Comment;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论服务 - 获取评论、发表评论、回复评论、点赞评论
 */
public class HanimeCommentService {

    private final OkHttpClient client;
    private final HanimeConfig config;
    private final Gson gson;

    public HanimeCommentService(OkHttpClient client, HanimeConfig config) {
        this.client = client;
        this.config = config;
        this.gson = new Gson();
    }

    // ======================== 获取评论 ========================

    /**
     * 获取评论列表
     * @param type 类型，如 "video", "preview"
     * @param code 影片代码
     * @return 评论列表
     */
    public List<Comment> getComments(String type, String code) throws HanimeException {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "loadComment").newBuilder()
                .addQueryParameter("type", type)
                .addQueryParameter("id", code)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("获取评论失败: " + response.code());
            }
            String json = response.body().string();
            return parseCommentsFromJson(json);
        } catch (IOException e) {
            throw new HanimeNetworkException(e.getMessage());
        }
    }

    /**
     * 获取影片评论
     */
    public List<Comment> getVideoComments(String videoCode) throws HanimeException {
        return getComments("video", videoCode);
    }

    // ======================== 获取评论回复 ========================

    /**
     * 获取评论回复
     * @param commentId 评论ID
     * @return 回复列表
     */
    public List<Comment> getReplies(String commentId) throws IOException {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "loadReplies").newBuilder()
                .addQueryParameter("id", commentId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("获取回复失败: " + response.code());
            }
            String json = response.body().string();
            return parseCommentsFromJson(json);
        }
    }

    // ======================== 发表评论 ========================

    /**
     * 发表评论
     */
    public boolean createComment(String csrfToken, String userId, String type,
                                  String foreignId, String text) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("comment-user-id", userId)
                .add("comment-type", type)
                .add("comment-foreign-id", foreignId)
                .add("comment-text", text)
                .add("comment-count", "1")
                .add("comment-is-political", "0")
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "createComment")
                .post(formBody)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== 回复评论 ========================

    /**
     * 回复评论
     */
    public boolean replyComment(String csrfToken, String commentId, String text) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("reply-comment-id", commentId)
                .add("reply-comment-text", text)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "replyComment")
                .post(formBody)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== 点赞评论 ========================

    /**
     * 点赞/踩评论
     */
    public boolean likeComment(String csrfToken, String foreignType, String foreignId,
                                int isPositive, String userId, int likesCount,
                                int likesSum, int likeStatus, int unlikeStatus) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("foreign_type", foreignType)
                .add("is_positive", String.valueOf(isPositive))
                .add("comment-likes-count", String.valueOf(likesCount))
                .add("comment-likes-sum", String.valueOf(likesSum))
                .add("like-comment-status", String.valueOf(likeStatus))
                .add("unlike-comment-status", String.valueOf(unlikeStatus));

        if (foreignId != null) {
            formBuilder.add("foreign_id", foreignId);
        }
        if (userId != null) {
            formBuilder.add("comment-like-user-id", userId);
        }

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "commentLike")
                .post(formBuilder.build())
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ======================== JSON解析 ========================

    private List<Comment> parseCommentsFromJson(String json) {
        List<Comment> comments = new ArrayList<>();
        try {
            JsonElement element = gson.fromJson(json, JsonElement.class);
            JsonArray array;
            if (element.isJsonArray()) {
                array = element.getAsJsonArray();
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("comments")) {
                    array = obj.getAsJsonArray("comments");
                } else if (obj.has("data")) {
                    array = obj.getAsJsonArray("data");
                } else {
                    for (String key : obj.keySet()) {
                        if (obj.get(key).isJsonArray()) {
                            array = obj.getAsJsonArray(key);
                            break;
                        }
                    }
                    return comments;
                }
            } else {
                return comments;
            }

            for (JsonElement item : array) {
                if (item.isJsonObject()) {
                    comments.add(parseSingleComment(item.getAsJsonObject()));
                }
            }
        } catch (Exception ignored) {
        }
        return comments;
    }

    private Comment parseSingleComment(JsonObject obj) {
        Comment comment = new Comment();

        if (obj.has("id")) comment.setId(getStringValue(obj, "id"));
        if (obj.has("text")) comment.setText(getStringValue(obj, "text"));
        else if (obj.has("content")) comment.setText(getStringValue(obj, "content"));
        if (obj.has("username")) comment.setUsername(getStringValue(obj, "username"));
        else if (obj.has("user")) comment.setUsername(getStringValue(obj, "user"));
        if (obj.has("avatar")) comment.setAvatarUrl(getStringValue(obj, "avatar"));
        else if (obj.has("avatar_url")) comment.setAvatarUrl(getStringValue(obj, "avatar_url"));
        if (obj.has("time")) comment.setTime(getStringValue(obj, "time"));
        else if (obj.has("created_at")) comment.setTime(getStringValue(obj, "created_at"));
        if (obj.has("likes")) comment.setLikes(getIntValue(obj, "likes"));
        if (obj.has("dislikes")) comment.setDislikes(getIntValue(obj, "dislikes"));

        return comment;
    }

    private String getStringValue(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }

    private int getIntValue(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsInt() : 0;
    }
}
