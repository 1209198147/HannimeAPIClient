package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile{
    // 用户ID
    private String id;
    // 用户名
    private String name;
    // 头像
    private String avatarUrl;
    // 订阅者数量
    @Builder.Default
    private int subscriberCount = 0;
    // 视频数量
    @Builder.Default
    private int videoCount = 0;
}
