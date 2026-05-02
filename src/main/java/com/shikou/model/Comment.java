package com.shikou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    /** 评论ID */
    private String id;

    /** 评论内容 */
    private String text;

    /** 评论者用户名 */
    private String username;

    /** 评论者头像URL */
    private String avatarUrl;

    /** 评论时间 */
    private String time;

    /** 点赞数 */
    @Builder.Default
    private int likes = 0;

    /** 踩数 */
    @Builder.Default
    private int dislikes = 0;

    /** 是否已点赞 */
    @Builder.Default
    private boolean liked = false;

    /** 是否已踩 */
    @Builder.Default
    private boolean disliked = false;

    /** 回复列表 */
    private List<Comment> replies;
}
