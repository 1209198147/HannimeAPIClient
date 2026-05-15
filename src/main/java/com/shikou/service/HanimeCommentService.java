package com.shikou.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.model.entities.Comment;
import com.shikou.parser.HtmlParser;
import com.shikou.util.HanimeHttpExecutor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
    public List<Comment> getComments(String type, String code) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getCommentsUrl(type, code);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String json = HanimeHttpExecutor.executeForString(client, request, "获取评论失败");
        return parseCommentsHtml(json, "comments");
    }

    @NotNull
    private HttpUrl getCommentsUrl(String type, String code) {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "loadComment").newBuilder()
                .addQueryParameter("type", type)
                .addQueryParameter("id", code)
                .addQueryParameter("content", "comment-tablink")
                .build();
        return url;
    }

    /**
     * 获取影片评论
     */
    public List<Comment> getVideoComments(String videoCode) throws HanimeNetworkException, HanimeApiException {
        return getComments("video", videoCode);
    }

    // ======================== 获取评论回复 ========================

    /**
     * 获取评论回复
     * @param commentId 评论ID
     * @return 回复列表
     */
    public List<Comment> getReplies(String commentId) throws HanimeNetworkException, HanimeApiException {
        HttpUrl url = getRepliesUrl(commentId);

        Request request = new Request.Builder()
                .url(url)
                .build();

        String json = HanimeHttpExecutor.executeForString(client, request, "获取回复失败");
        return parseCommentsHtml(json, "replies");
    }

    @NotNull
    private HttpUrl getRepliesUrl(String commentId) {
        HttpUrl url = HttpUrl.parse(config.getBaseUrl() + "loadReplies").newBuilder()
                .addQueryParameter("id", commentId)
                .build();
        return url;
    }

    // ======================== 发表评论 ========================

    /**
     * 发表评论
     */
    public boolean createComment(String csrfToken, String userId, String type,
                                  String foreignId, String text) throws HanimeException {
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
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new HanimeNetworkException("发表评论失败: " + e.getMessage());
        }
    }

    // ======================== 回复评论 ========================

    /**
     * 回复评论
     */
    public boolean replyComment(String csrfToken, String commentId, String text) throws HanimeException {
        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("reply-comment-id", commentId)
                .add("reply-comment-text", text)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "replyComment")
                .post(formBody)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new HanimeNetworkException("回复评论失败: " + e.getMessage());
        }
    }

    // ======================== 点赞评论 ========================

    /**
     * 点赞/踩评论
     */
    public boolean likeComment(String csrfToken, String foreignType, String foreignId,
                                int isPositive, String userId, int likesCount,
                                int likesSum, int likeStatus, int unlikeStatus) throws HanimeException {
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
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new HanimeNetworkException("点赞评论失败: " + e.getMessage());
        }
    }

    // ======================== HTML解析 ========================

    /**
     * 从 JSON 响应中提取指定字段的 HTML 并解析为评论列表
     * @param json     JSON 响应字符串
     * @param htmlKey  HTML 字段名（"comments" 或 "replies"）
     * @return 评论列表
     */
    private List<Comment> parseCommentsHtml(String json, String htmlKey) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj == null || !obj.has(htmlKey)) {
            return List.of();
        }
        String html = obj.get(htmlKey).getAsString();
        return HtmlParser.parseComments(html);
    }
}
